package br.com.caisora.contrato.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.contrato.api.AtivarContratoRequest;
import br.com.caisora.contrato.api.AtualizarContratoRequest;
import br.com.caisora.contrato.api.ContratoResponse;
import br.com.caisora.contrato.api.CriarContratoRequest;
import br.com.caisora.contrato.api.EncerrarContratoRequest;
import br.com.caisora.contrato.api.HistoricoContratoResponse;
import br.com.caisora.contrato.api.VincularOcupacaoContratoRequest;
import br.com.caisora.contrato.api.VinculoContratoOcupacaoResponse;
import br.com.caisora.contrato.dominio.Contrato;
import br.com.caisora.contrato.dominio.ContratoRepository;
import br.com.caisora.contrato.dominio.HistoricoContrato;
import br.com.caisora.contrato.dominio.HistoricoContratoRepository;
import br.com.caisora.contrato.dominio.PeriodicidadeContrato;
import br.com.caisora.contrato.dominio.SequenciaContratoRepository;
import br.com.caisora.contrato.dominio.StatusContrato;
import br.com.caisora.contrato.dominio.TipoEventoContrato;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacao;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacaoRepository;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContratoService {

    private static final Set<StatusContrato> STATUS_COMERCIAIS_ATIVOS =
        EnumSet.of(
            StatusContrato.ATIVO,
            StatusContrato.SUSPENSO,
            StatusContrato.EM_ENCERRAMENTO
        );

    private final ContratoRepository contratoRepository;
    private final VinculoContratoOcupacaoRepository vinculoRepository;
    private final HistoricoContratoRepository historicoRepository;
    private final SequenciaContratoRepository sequenciaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ClienteRepository clienteRepository;
    private final EmbarcacaoRepository embarcacaoRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoMapper contratoMapper;
    private final LeitorTokenJwt leitorTokenJwt;

    public ContratoService(
        ContratoRepository contratoRepository,
        VinculoContratoOcupacaoRepository vinculoRepository,
        HistoricoContratoRepository historicoRepository,
        SequenciaContratoRepository sequenciaRepository,
        OrganizacaoRepository organizacaoRepository,
        ClienteRepository clienteRepository,
        EmbarcacaoRepository embarcacaoRepository,
        OcupacaoRepository ocupacaoRepository,
        UsuarioRepository usuarioRepository,
        ContratoMapper contratoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.contratoRepository = contratoRepository;
        this.vinculoRepository = vinculoRepository;
        this.historicoRepository = historicoRepository;
        this.sequenciaRepository = sequenciaRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.clienteRepository = clienteRepository;
        this.embarcacaoRepository = embarcacaoRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoMapper = contratoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public ContratoResponse criar(CriarContratoRequest request) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        validarDadosComerciais(
            request.periodicidade(),
            request.dataInicio(),
            request.dataFim(),
            request.diasAvisoPrevio(),
            request.valorBase(),
            request.diaVencimento()
        );

        Cliente cliente = buscarCliente(
            contexto.organizacaoId(),
            request.clienteId()
        );
        Embarcacao embarcacao = buscarEmbarcacao(
            contexto.organizacaoId(),
            request.embarcacaoId()
        );
        validarPropriedade(cliente, embarcacao);

        String numero = gerarNumeroContrato(
            contexto.organizacaoId(),
            request.dataInicio().getYear()
        );

        Contrato contrato = new Contrato(
            contexto.organizacao(),
            numero,
            cliente,
            embarcacao,
            request.tipoVagaContratada(),
            request.periodicidade(),
            request.dataInicio(),
            request.dataFim(),
            request.renovacaoAutomatica(),
            request.diasAvisoPrevio(),
            request.valorBase(),
            request.diaVencimento(),
            normalizarTextoOpcional(request.observacoes()),
            contexto.usuario()
        );

        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            TipoEventoContrato.CRIADO,
            null,
            StatusContrato.RASCUNHO,
            "Contrato criado",
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ContratoResponse> listar(
        StatusContrato status,
        UUID clienteId,
        UUID embarcacaoId,
        Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();
        return contratoRepository
            .buscarPorFiltros(
                organizacaoId,
                status,
                clienteId,
                embarcacaoId,
                paginacao
            )
            .map(contratoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public ContratoResponse buscarPorId(UUID id) {
        UUID organizacaoId = obterOrganizacaoAutenticada();
        return contratoMapper.paraResponse(
            buscarContrato(organizacaoId, id)
        );
    }

    @Transactional
    public ContratoResponse atualizar(
        UUID id,
        AtualizarContratoRequest request
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        validarContratoEditavel(contrato);
        validarDadosComerciais(
            request.periodicidade(),
            request.dataInicio(),
            request.dataFim(),
            request.diasAvisoPrevio(),
            request.valorBase(),
            request.diaVencimento()
        );

        Cliente cliente = buscarCliente(
            contexto.organizacaoId(),
            request.clienteId()
        );
        Embarcacao embarcacao = buscarEmbarcacao(
            contexto.organizacaoId(),
            request.embarcacaoId()
        );
        validarPropriedade(cliente, embarcacao);

        contrato.atualizarDados(
            cliente,
            embarcacao,
            request.tipoVagaContratada(),
            request.periodicidade(),
            request.dataInicio(),
            request.dataFim(),
            request.renovacaoAutomatica(),
            request.diasAvisoPrevio(),
            request.valorBase(),
            request.diaVencimento(),
            normalizarTextoOpcional(request.observacoes())
        );

        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            TipoEventoContrato.EDITADO,
            StatusContrato.RASCUNHO,
            StatusContrato.RASCUNHO,
            "Dados comerciais atualizados",
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional
    public ContratoResponse enviarParaAssinatura(
        UUID id,
        String descricao
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        StatusContrato anterior = contrato.getStatus();
        executarTransicao(
            contrato::enviarParaAssinatura,
            "Contrato precisa estar em rascunho"
        );
        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            TipoEventoContrato.ENVIADO_PARA_ASSINATURA,
            anterior,
            salvo.getStatus(),
            descricaoOuPadrao(descricao, "Contrato enviado para assinatura"),
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional
    public ContratoResponse ativar(UUID id, AtivarContratoRequest request) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        validarStatus(
            contrato,
            StatusContrato.PENDENTE_ASSINATURA,
            "Contrato precisa estar pendente de assinatura para ser ativado"
        );
        validarAtivacao(contrato, contexto.organizacaoId());
        StatusContrato anterior = contrato.getStatus();

        try {
            contrato.ativar(request.dataAssinatura());
        } catch (IllegalArgumentException exception) {
            throw new DadosInvalidosException(
                "ATIVACAO_CONTRATO_INVALIDA",
                exception.getMessage()
            );
        }

        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            TipoEventoContrato.ATIVADO,
            anterior,
            salvo.getStatus(),
            descricaoOuPadrao(request.descricao(), "Contrato ativado"),
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional
    public ContratoResponse suspender(UUID id, String descricao) {
        return alterarStatusSimples(
            id,
            TipoEventoContrato.SUSPENSO,
            descricao,
            "Contrato suspenso",
            Contrato::suspender
        );
    }

    @Transactional
    public ContratoResponse reativar(UUID id, String descricao) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        validarStatus(
            contrato,
            StatusContrato.SUSPENSO,
            "Somente contrato suspenso pode ser reativado"
        );
        validarAtivacao(contrato, contexto.organizacaoId());
        return alterarStatusSimples(
            contrato,
            contexto,
            TipoEventoContrato.REATIVADO,
            descricao,
            "Contrato reativado",
            Contrato::reativar
        );
    }

    @Transactional
    public ContratoResponse solicitarEncerramento(
        UUID id,
        String descricao
    ) {
        return alterarStatusSimples(
            id,
            TipoEventoContrato.ENCERRAMENTO_SOLICITADO,
            descricao,
            "Encerramento solicitado",
            Contrato::solicitarEncerramento
        );
    }

    @Transactional
    public ContratoResponse encerrar(
        UUID id,
        EncerrarContratoRequest request
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        validarStatus(
            contrato,
            StatusContrato.EM_ENCERRAMENTO,
            "Contrato precisa estar em encerramento para ser encerrado"
        );
        StatusContrato anterior = contrato.getStatus();

        try {
            contrato.encerrar(request.dataEncerramento());
        } catch (IllegalArgumentException exception) {
            throw new DadosInvalidosException(
                "ENCERRAMENTO_CONTRATO_INVALIDO",
                exception.getMessage()
            );
        }

        vinculoRepository
            .findFirstByContratoIdAndOrganizacaoIdAndFimEmIsNull(
                id,
                contexto.organizacaoId()
            )
            .ifPresent(vinculo -> {
                vinculo.finalizar("Contrato encerrado");
                vinculoRepository.save(vinculo);
                registrarHistorico(
                    contrato,
                    TipoEventoContrato.OCUPACAO_DESVINCULADA,
                    anterior,
                    contrato.getStatus(),
                    "Ocupacao desvinculada pelo encerramento do contrato",
                    contexto.usuario()
                );
            });

        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            TipoEventoContrato.ENCERRADO,
            anterior,
            salvo.getStatus(),
            descricaoOuPadrao(request.descricao(), "Contrato encerrado"),
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional
    public ContratoResponse cancelar(UUID id, String descricao) {
        return alterarStatusSimples(
            id,
            TipoEventoContrato.CANCELADO,
            descricao,
            "Contrato cancelado",
            Contrato::cancelar
        );
    }

    @Transactional
    public VinculoContratoOcupacaoResponse vincularOcupacao(
        UUID contratoId,
        VincularOcupacaoContratoRequest request
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(
            contexto.organizacaoId(),
            contratoId
        );
        if (!contrato.podeReceberOcupacao()) {
            throw new ConflitoDadosException(
                "Somente contrato ativo pode receber uma ocupacao"
            );
        }

        Ocupacao ocupacao = buscarOcupacao(
            contexto.organizacaoId(),
            request.ocupacaoId()
        );
        if (!ocupacao.estaAtiva()) {
            throw new ConflitoDadosException(
                "Somente ocupacao ativa pode ser vinculada ao contrato"
            );
        }
        if (
            !ocupacao.getEmbarcacao().getId()
                .equals(contrato.getEmbarcacao().getId())
        ) {
            throw new DadosInvalidosException(
                "OCUPACAO_EMBARCACAO_DIVERGENTE",
                "A ocupacao pertence a outra embarcacao"
            );
        }

        validarVinculosAbertos(
            contexto.organizacaoId(),
            contratoId,
            ocupacao.getId()
        );

        VinculoContratoOcupacao vinculo =
            new VinculoContratoOcupacao(
                contexto.organizacao(),
                contrato,
                ocupacao
            );
        VinculoContratoOcupacao salvo = vinculoRepository.save(vinculo);
        registrarHistorico(
            contrato,
            TipoEventoContrato.OCUPACAO_VINCULADA,
            contrato.getStatus(),
            contrato.getStatus(),
            "Ocupacao vinculada ao contrato",
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional
    public VinculoContratoOcupacaoResponse desvincularOcupacao(
        UUID contratoId,
        UUID vinculoId,
        String motivo
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(
            contexto.organizacaoId(),
            contratoId
        );
        VinculoContratoOcupacao vinculo = vinculoRepository
            .findByIdAndContratoIdAndOrganizacaoId(
                vinculoId,
                contratoId,
                contexto.organizacaoId()
            )
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Vinculo de ocupacao nao encontrado"
            ));

        String motivoNormalizado = normalizarTextoComLimite(
            motivo,
            "Ocupacao desvinculada",
            500,
            "Motivo do fim do vinculo"
        );
        try {
            vinculo.finalizar(motivoNormalizado);
        } catch (IllegalStateException exception) {
            throw new ConflitoDadosException(exception.getMessage());
        }

        VinculoContratoOcupacao salvo = vinculoRepository.save(vinculo);
        registrarHistorico(
            contrato,
            TipoEventoContrato.OCUPACAO_DESVINCULADA,
            contrato.getStatus(),
            contrato.getStatus(),
            motivoNormalizado,
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<VinculoContratoOcupacaoResponse> listarOcupacoes(
        UUID contratoId
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();
        buscarContrato(organizacaoId, contratoId);
        return vinculoRepository
            .findAllByContratoIdAndOrganizacaoIdOrderByInicioEmDesc(
                contratoId,
                organizacaoId
            )
            .stream()
            .map(contratoMapper::paraResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<HistoricoContratoResponse> listarHistorico(
        UUID contratoId,
        Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();
        buscarContrato(organizacaoId, contratoId);
        return historicoRepository
            .findAllByContratoIdAndOrganizacaoIdOrderByRealizadoEmDesc(
                contratoId,
                organizacaoId,
                paginacao
            )
            .map(contratoMapper::paraResponse);
    }

    private ContratoResponse alterarStatusSimples(
        UUID id,
        TipoEventoContrato evento,
        String descricao,
        String descricaoPadrao,
        AcaoContrato acao
    ) {
        ContextoAutenticado contexto = obterContextoAutenticado();
        Contrato contrato = buscarContrato(contexto.organizacaoId(), id);
        return alterarStatusSimples(
            contrato,
            contexto,
            evento,
            descricao,
            descricaoPadrao,
            acao
        );
    }

    private ContratoResponse alterarStatusSimples(
        Contrato contrato,
        ContextoAutenticado contexto,
        TipoEventoContrato evento,
        String descricao,
        String descricaoPadrao,
        AcaoContrato acao
    ) {
        StatusContrato anterior = contrato.getStatus();
        executarTransicao(
            () -> acao.executar(contrato),
            "Transicao de status invalida"
        );
        Contrato salvo = contratoRepository.save(contrato);
        registrarHistorico(
            salvo,
            evento,
            anterior,
            salvo.getStatus(),
            descricaoOuPadrao(descricao, descricaoPadrao),
            contexto.usuario()
        );
        return contratoMapper.paraResponse(salvo);
    }

    private void validarAtivacao(
        Contrato contrato,
        UUID organizacaoId
    ) {
        validarPropriedade(
            contrato.getCliente(),
            contrato.getEmbarcacao()
        );
        if (!contrato.getCliente().isAtivo()) {
            throw new ConflitoDadosException(
                "Nao e possivel ativar contrato com cliente inativo"
            );
        }
        if (!contrato.getEmbarcacao().isAtiva()) {
            throw new ConflitoDadosException(
                "Nao e possivel ativar contrato com embarcacao inativa"
            );
        }
        validarContratoAtivoDuplicado(contrato, organizacaoId);
    }

    private void validarContratoAtivoDuplicado(
        Contrato contrato,
        UUID organizacaoId
    ) {
        boolean existe = contratoRepository
            .existsByOrganizacaoIdAndEmbarcacaoIdAndStatusInAndIdNot(
                organizacaoId,
                contrato.getEmbarcacao().getId(),
                STATUS_COMERCIAIS_ATIVOS,
                contrato.getId()
            );
        if (existe) {
            throw new ConflitoDadosException(
                "A embarcacao ja possui outro contrato comercial ativo"
            );
        }
    }

    private void validarVinculosAbertos(
        UUID organizacaoId,
        UUID contratoId,
        UUID ocupacaoId
    ) {
        if (
            vinculoRepository
                .findFirstByContratoIdAndOrganizacaoIdAndFimEmIsNull(
                    contratoId,
                    organizacaoId
                )
                .isPresent()
        ) {
            throw new ConflitoDadosException(
                "O contrato ja possui uma ocupacao vinculada"
            );
        }
        if (
            vinculoRepository
                .findFirstByOcupacaoIdAndOrganizacaoIdAndFimEmIsNull(
                    ocupacaoId,
                    organizacaoId
                )
                .isPresent()
        ) {
            throw new ConflitoDadosException(
                "A ocupacao ja esta vinculada a outro contrato"
            );
        }
    }

    private void validarStatus(
        Contrato contrato,
        StatusContrato esperado,
        String mensagem
    ) {
        if (contrato.getStatus() != esperado) {
            throw new ConflitoDadosException(mensagem);
        }
    }

    private void validarContratoEditavel(Contrato contrato) {
        if (!contrato.podeSerEditado()) {
            throw new ConflitoDadosException(
                "Somente contrato em rascunho pode ser editado"
            );
        }
    }

    private void validarPropriedade(
        Cliente cliente,
        Embarcacao embarcacao
    ) {
        if (
            !embarcacao.getProprietario().getId()
                .equals(cliente.getId())
        ) {
            throw new DadosInvalidosException(
                "PROPRIETARIO_EMBARCACAO_INVALIDO",
                "A embarcacao nao pertence ao cliente informado"
            );
        }
    }

    private void validarDadosComerciais(
        PeriodicidadeContrato periodicidade,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer diasAvisoPrevio,
        BigDecimal valorBase,
        Integer diaVencimento
    ) {
        if (periodicidade == null) {
            throw new DadosInvalidosException(
                "PERIODICIDADE_OBRIGATORIA",
                "Periodicidade obrigatoria"
            );
        }
        if (dataInicio == null) {
            throw new DadosInvalidosException(
                "DATA_INICIO_OBRIGATORIA",
                "Data de inicio obrigatoria"
            );
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new DadosInvalidosException(
                "PERIODO_CONTRATO_INVALIDO",
                "Data de fim nao pode ser anterior a data de inicio"
            );
        }
        if (
            periodicidade == PeriodicidadeContrato.PERSONALIZADA
                && dataFim == null
        ) {
            throw new DadosInvalidosException(
                "DATA_FIM_OBRIGATORIA",
                "Data de fim obrigatoria para periodicidade personalizada"
            );
        }
        if (diasAvisoPrevio != null && diasAvisoPrevio < 0) {
            throw new DadosInvalidosException(
                "AVISO_PREVIO_INVALIDO",
                "Dias de aviso previo nao pode ser negativo"
            );
        }
        if (valorBase == null || valorBase.signum() < 0) {
            throw new DadosInvalidosException(
                "VALOR_BASE_INVALIDO",
                "Valor base deve ser maior ou igual a zero"
            );
        }
        if (
            diaVencimento == null
                || diaVencimento < 1
                || diaVencimento > 31
        ) {
            throw new DadosInvalidosException(
                "DIA_VENCIMENTO_INVALIDO",
                "Dia de vencimento deve estar entre 1 e 31"
            );
        }
    }

    private Contrato buscarContrato(UUID organizacaoId, UUID id) {
        return contratoRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Contrato nao encontrado"
            ));
    }

    private Cliente buscarCliente(UUID organizacaoId, UUID id) {
        return clienteRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente nao encontrado"
            ));
    }

    private Embarcacao buscarEmbarcacao(
        UUID organizacaoId,
        UUID id
    ) {
        return embarcacaoRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Embarcacao nao encontrada"
            ));
    }

    private Ocupacao buscarOcupacao(UUID organizacaoId, UUID id) {
        return ocupacaoRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Ocupacao nao encontrada"
            ));
    }

    private ContextoAutenticado obterContextoAutenticado() {
        UsuarioAutenticado autenticado =
            leitorTokenJwt.obterUsuarioAutenticado();
        Organizacao organizacao = organizacaoRepository
            .findById(autenticado.organizacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Organizacao nao encontrada"
            ));
        Usuario usuario = usuarioRepository
            .findByIdAndOrganizacaoId(
                autenticado.id(),
                autenticado.organizacaoId()
            )
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Usuario autenticado nao encontrado"
            ));
        return new ContextoAutenticado(
            autenticado.organizacaoId(),
            organizacao,
            usuario
        );
    }

    private UUID obterOrganizacaoAutenticada() {
        return leitorTokenJwt
            .obterUsuarioAutenticado()
            .organizacaoId();
    }

    private String gerarNumeroContrato(
        UUID organizacaoId,
        int ano
    ) {
        long sequencial = sequenciaRepository
            .proximoNumero(organizacaoId, ano);
        return "CTR-%d-%06d".formatted(ano, sequencial);
    }

    private void registrarHistorico(
        Contrato contrato,
        TipoEventoContrato evento,
        StatusContrato statusAnterior,
        StatusContrato statusNovo,
        String descricao,
        Usuario usuario
    ) {
        historicoRepository.save(
            new HistoricoContrato(
                contrato.getOrganizacao(),
                contrato,
                evento,
                statusAnterior,
                statusNovo,
                normalizarTextoOpcional(descricao),
                usuario
            )
        );
    }

    private void executarTransicao(
        Runnable acao,
        String mensagemPadrao
    ) {
        try {
            acao.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ConflitoDadosException(
                exception.getMessage() == null
                    ? mensagemPadrao
                    : exception.getMessage()
            );
        }
    }

    private String descricaoOuPadrao(
        String descricao,
        String padrao
    ) {
        return normalizarTextoComLimite(
            descricao,
            padrao,
            1000,
            "Descricao"
        );
    }

    private String normalizarTextoComLimite(
        String texto,
        String padrao,
        int limite,
        String campo
    ) {
        String normalizado = normalizarTextoOpcional(texto);
        String resultado = normalizado == null ? padrao : normalizado;
        if (resultado.length() > limite) {
            throw new DadosInvalidosException(
                "TEXTO_EXCEDE_LIMITE",
                campo + " deve possuir no maximo " + limite + " caracteres"
            );
        }
        return resultado;
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    @FunctionalInterface
    private interface AcaoContrato {
        void executar(Contrato contrato);
    }

    private record ContextoAutenticado(
        UUID organizacaoId,
        Organizacao organizacao,
        Usuario usuario
    ) {
    }
}
