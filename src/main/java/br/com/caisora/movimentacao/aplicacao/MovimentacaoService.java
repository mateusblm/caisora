package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.movimentacao.api.AtualizarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.CancelarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.ConcluirMovimentacaoRequest;
import br.com.caisora.movimentacao.api.CriarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.HistoricoMovimentacaoResponse;
import br.com.caisora.movimentacao.api.IniciarMovimentacaoRequest;
import br.com.caisora.movimentacao.api.MovimentacaoResponse;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacao;
import br.com.caisora.movimentacao.dominio.HistoricoMovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.Movimentacao;
import br.com.caisora.movimentacao.dominio.MovimentacaoRepository;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacaoRepository;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentacaoService {

    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(1);

    private static final Collection<StatusMovimentacao> STATUS_ABERTOS = List.of(
        StatusMovimentacao.AGENDADA,
        StatusMovimentacao.EM_EXECUCAO
    );

    private final MovimentacaoRepository movimentacaoRepository;
    private final PosicaoEmbarcacaoRepository posicaoRepository;
    private final HistoricoMovimentacaoRepository historicoRepository;
    private final EmbarcacaoRepository embarcacaoRepository;
    private final VagaRepository vagaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final PosicaoEmbarcacaoService posicaoService;
    private final MovimentacaoMapper movimentacaoMapper;
    private final HistoricoMovimentacaoMapper historicoMapper;
    private final LeitorTokenJwt leitorTokenJwt;

    public MovimentacaoService(
        MovimentacaoRepository movimentacaoRepository,
        PosicaoEmbarcacaoRepository posicaoRepository,
        HistoricoMovimentacaoRepository historicoRepository,
        EmbarcacaoRepository embarcacaoRepository,
        VagaRepository vagaRepository,
        UsuarioRepository usuarioRepository,
        OcupacaoRepository ocupacaoRepository,
        PosicaoEmbarcacaoService posicaoService,
        MovimentacaoMapper movimentacaoMapper,
        HistoricoMovimentacaoMapper historicoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.posicaoRepository = posicaoRepository;
        this.historicoRepository = historicoRepository;
        this.embarcacaoRepository = embarcacaoRepository;
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.posicaoService = posicaoService;
        this.movimentacaoMapper = movimentacaoMapper;
        this.historicoMapper = historicoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public MovimentacaoResponse criar(CriarMovimentacaoRequest request) {
        validarRequest(request);

        UsuarioAutenticado autenticado = obterUsuarioAutenticado();
        UUID organizacaoId = autenticado.organizacaoId();

        validarAgendamento(request.agendadaPara());

        Embarcacao embarcacao = buscarEmbarcacao(
            request.embarcacaoId(),
            organizacaoId
        );
        validarEmbarcacaoAtiva(embarcacao);

        Usuario solicitante = buscarUsuarioAtivo(
            autenticado.id(),
            organizacaoId
        );
        Usuario operador = buscarOperadorOpcional(
            request.operadorResponsavelId(),
            organizacaoId
        );

        validarSemMovimentacaoAberta(
            organizacaoId,
            embarcacao.getId()
        );

        PosicaoEmbarcacao posicao = posicaoService.obterOuCriarPosicao(
            embarcacao,
            organizacaoId
        );

        DestinoResolvido destino = resolverDestino(
            request.tipo(),
            request.tipoPosicaoDestino(),
            request.vagaDestinoId(),
            embarcacao,
            organizacaoId,
            null
        );

        validarCoerenciaOperacional(
            request.tipo(),
            posicao,
            destino,
            embarcacao,
            organizacaoId
        );

        Movimentacao movimentacao = criarEntidade(
            embarcacao,
            request,
            posicao,
            destino,
            solicitante,
            operador
        );

        salvarMovimentacao(movimentacao);

        historicoRepository.save(
            HistoricoMovimentacao.criada(
                movimentacao,
                solicitante,
                Instant.now()
            )
        );

        return movimentacaoMapper.paraResponse(movimentacao);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(
        StatusMovimentacao status,
        TipoMovimentacao tipo,
        UUID embarcacaoId,
        Instant inicio,
        Instant fim,
        Pageable paginacao
    ) {
        validarFiltrosListagem(
            status,
            tipo,
            embarcacaoId,
            inicio,
            fim
        );

        UUID organizacaoId = obterUsuarioAutenticado().organizacaoId();

        Page<Movimentacao> pagina;

        if (status != null) {
            pagina = movimentacaoRepository
                .findAllByOrganizacaoIdAndStatus(
                    organizacaoId,
                    status,
                    paginacao
                );
        } else if (tipo != null) {
            pagina = movimentacaoRepository
                .findAllByOrganizacaoIdAndTipo(
                    organizacaoId,
                    tipo,
                    paginacao
                );
        } else if (embarcacaoId != null) {
            pagina = movimentacaoRepository
                .findAllByOrganizacaoIdAndEmbarcacaoId(
                    organizacaoId,
                    embarcacaoId,
                    paginacao
                );
        } else if (inicio != null) {
            pagina = movimentacaoRepository
                .findAllByOrganizacaoIdAndAgendadaParaBetween(
                    organizacaoId,
                    inicio,
                    fim,
                    paginacao
                );
        } else {
            pagina = movimentacaoRepository.findAllByOrganizacaoId(
                organizacaoId,
                paginacao
            );
        }

        return pagina.map(movimentacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public MovimentacaoResponse buscarPorId(UUID id) {
        UUID organizacaoId = obterUsuarioAutenticado().organizacaoId();

        return movimentacaoMapper.paraResponse(
            buscarMovimentacao(id, organizacaoId)
        );
    }

    @Transactional
    public MovimentacaoResponse atualizar(
        UUID id,
        AtualizarMovimentacaoRequest request
    ) {
        validarRequest(request);

        UsuarioAutenticado autenticado = obterUsuarioAutenticado();
        UUID organizacaoId = autenticado.organizacaoId();

        validarAgendamento(request.agendadaPara());

        Movimentacao movimentacao = buscarMovimentacao(
            id,
            organizacaoId
        );

        Usuario usuario = buscarUsuarioAtivo(
            autenticado.id(),
            organizacaoId
        );

        Usuario operador = buscarOperadorOpcional(
            request.operadorResponsavelId(),
            organizacaoId
        );

        DestinoResolvido destino = resolverDestino(
            movimentacao.getTipo(),
            request.tipoPosicaoDestino(),
            request.vagaDestinoId(),
            movimentacao.getEmbarcacao(),
            organizacaoId,
            movimentacao.getId()
        );

        PosicaoEmbarcacao posicao = buscarPosicao(
            movimentacao.getEmbarcacao(),
            organizacaoId
        );

        validarCoerenciaOperacional(
            movimentacao.getTipo(),
            posicao,
            destino,
            movimentacao.getEmbarcacao(),
            organizacaoId
        );

        Instant agendamentoAnterior = movimentacao.getAgendadaPara();
        Map<String, Object> dadosAnteriores = criarSnapshotAtualizavel(
            movimentacao
        );

        executarRegraDominio(
            () -> movimentacao.atualizarDados(
                request.prioridade(),
                request.tipoPosicaoDestino(),
                destino.vaga(),
                request.descricaoDestino(),
                request.agendadaPara(),
                operador,
                request.observacoes()
            )
        );

        Map<String, Object> dadosNovos = criarSnapshotAtualizavel(
            movimentacao
        );

        salvarMovimentacao(movimentacao);

        Instant ocorridoEm = Instant.now();

        if (!Objects.equals(
            agendamentoAnterior,
            movimentacao.getAgendadaPara()
        )) {
            historicoRepository.save(
                HistoricoMovimentacao.reagendada(
                    movimentacao,
                    usuario,
                    agendamentoAnterior,
                    movimentacao.getAgendadaPara(),
                    null,
                    ocorridoEm
                )
            );
        }

        if (!Objects.equals(dadosAnteriores, dadosNovos)) {
            historicoRepository.save(
                HistoricoMovimentacao.atualizada(
                    movimentacao,
                    usuario,
                    dadosAnteriores,
                    dadosNovos,
                    null,
                    ocorridoEm
                )
            );
        }

        return movimentacaoMapper.paraResponse(movimentacao);
    }

    @Transactional
    public MovimentacaoResponse iniciar(
        UUID id,
        IniciarMovimentacaoRequest request
    ) {
        validarRequest(request);

        UsuarioAutenticado autenticado = obterUsuarioAutenticado();
        UUID organizacaoId = autenticado.organizacaoId();

        Movimentacao movimentacao = buscarMovimentacao(
            id,
            organizacaoId
        );

        Usuario operador = buscarUsuarioAtivo(
            autenticado.id(),
            organizacaoId
        );

        validarDataOperacional(
            request.iniciadaEm(),
            movimentacao.getCriadaEm(),
            "INICIO_MOVIMENTACAO_INVALIDO",
            "O inicio da movimentacao"
        );

        executarRegraDominio(
            () -> movimentacao.iniciar(
                operador,
                request.iniciadaEm()
            )
        );

        salvarMovimentacao(movimentacao);

        historicoRepository.save(
            HistoricoMovimentacao.iniciada(
                movimentacao,
                operador,
                request.observacao(),
                request.iniciadaEm()
            )
        );

        return movimentacaoMapper.paraResponse(movimentacao);
    }

    @Transactional
    public MovimentacaoResponse concluir(
        UUID id,
        ConcluirMovimentacaoRequest request
    ) {
        validarRequest(request);

        UsuarioAutenticado autenticado = obterUsuarioAutenticado();
        UUID organizacaoId = autenticado.organizacaoId();

        Movimentacao movimentacao = buscarMovimentacao(
            id,
            organizacaoId
        );

        Usuario operador = buscarUsuarioAtivo(
            autenticado.id(),
            organizacaoId
        );

        validarDataOperacional(
            request.concluidaEm(),
            movimentacao.getIniciadaEm(),
            "CONCLUSAO_MOVIMENTACAO_INVALIDA",
            "A conclusao da movimentacao"
        );

        PosicaoEmbarcacao posicao = buscarPosicao(
            movimentacao.getEmbarcacao(),
            organizacaoId
        );

        validarPosicaoAindaCorrespondeOrigem(
            movimentacao,
            posicao
        );
        validarDestinoNaConclusao(movimentacao, organizacaoId);

        executarRegraDominio(
            () -> movimentacao.concluir(
                operador,
                request.concluidaEm()
            )
        );

        aplicarEfeitosDaConclusao(
            movimentacao,
            posicao,
            request.concluidaEm(),
            organizacaoId
        );

        salvarMovimentacao(movimentacao);

        historicoRepository.save(
            HistoricoMovimentacao.concluida(
                movimentacao,
                operador,
                request.observacao(),
                request.concluidaEm()
            )
        );

        return movimentacaoMapper.paraResponse(movimentacao);
    }

    @Transactional
    public MovimentacaoResponse cancelar(
        UUID id,
        CancelarMovimentacaoRequest request
    ) {
        validarRequest(request);

        UsuarioAutenticado autenticado = obterUsuarioAutenticado();
        UUID organizacaoId = autenticado.organizacaoId();

        Movimentacao movimentacao = buscarMovimentacao(
            id,
            organizacaoId
        );

        Usuario usuario = buscarUsuarioAtivo(
            autenticado.id(),
            organizacaoId
        );

        validarDataOperacional(
            request.canceladaEm(),
            movimentacao.getCriadaEm(),
            "CANCELAMENTO_MOVIMENTACAO_INVALIDO",
            "O cancelamento da movimentacao"
        );

        executarRegraDominio(
            () -> movimentacao.cancelar(
                request.canceladaEm(),
                request.motivo()
            )
        );

        salvarMovimentacao(movimentacao);

        historicoRepository.save(
            HistoricoMovimentacao.cancelada(
                movimentacao,
                usuario,
                request.motivo(),
                request.canceladaEm()
            )
        );

        return movimentacaoMapper.paraResponse(movimentacao);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoMovimentacaoResponse> listarHistorico(
        UUID movimentacaoId,
        Pageable paginacao
    ) {
        UUID organizacaoId = obterUsuarioAutenticado().organizacaoId();

        buscarMovimentacao(movimentacaoId, organizacaoId);

        return historicoRepository
            .findAllByOrganizacaoIdAndMovimentacaoId(
                organizacaoId,
                movimentacaoId,
                paginacao
            )
            .map(historicoMapper::paraResponse);
    }

    private Movimentacao criarEntidade(
        Embarcacao embarcacao,
        CriarMovimentacaoRequest request,
        PosicaoEmbarcacao posicao,
        DestinoResolvido destino,
        Usuario solicitante,
        Usuario operador
    ) {
        try {
            return new Movimentacao(
                embarcacao.getOrganizacao(),
                embarcacao,
                request.tipo(),
                request.prioridade(),
                posicao.getTipo(),
                posicao.getVaga(),
                posicao.getDescricaoLocal(),
                request.tipoPosicaoDestino(),
                destino.vaga(),
                request.descricaoDestino(),
                request.agendadaPara(),
                solicitante,
                operador,
                request.observacoes()
            );
        } catch (IllegalArgumentException | NullPointerException excecao) {
            throw new DadosInvalidosException(
                "MOVIMENTACAO_INVALIDA",
                excecao.getMessage()
            );
        }
    }

    private void aplicarEfeitosDaConclusao(
        Movimentacao movimentacao,
        PosicaoEmbarcacao posicao,
        Instant concluidaEm,
        UUID organizacaoId
    ) {
        if (movimentacao.getTipo() == TipoMovimentacao.TRANSFERENCIA) {
            transferirOcupacao(
                movimentacao,
                concluidaEm,
                organizacaoId
            );
        }

        posicao.atualizar(
            movimentacao.getTipoPosicaoDestino(),
            movimentacao.getVagaDestino(),
            movimentacao.getDescricaoDestino(),
            movimentacao
        );

        posicaoRepository.save(posicao);
    }

    private void transferirOcupacao(
        Movimentacao movimentacao,
        Instant concluidaEm,
        UUID organizacaoId
    ) {
        Ocupacao ocupacaoAtual = buscarOcupacaoAtivaDaEmbarcacao(
            organizacaoId,
            movimentacao.getEmbarcacao().getId()
        );

        if (!Objects.equals(
            ocupacaoAtual.getVaga().getId(),
            movimentacao.getVagaOrigem().getId()
        )) {
            throw new ConflitoDadosException(
                "A ocupacao ativa nao corresponde a vaga de origem"
            );
        }

        Instant fimPrevisto = ocupacaoAtual.getFimPrevistoEm();

        if (fimPrevisto != null && !fimPrevisto.isAfter(concluidaEm)) {
            fimPrevisto = null;
        }

        String observacoes = criarObservacaoTransferencia(
            ocupacaoAtual.getObservacoes(),
            movimentacao.getId()
        );

        ocupacaoAtual.encerrar(concluidaEm);
        ocupacaoRepository.save(ocupacaoAtual);

        /*
         * Forca o UPDATE antes do INSERT.
         * Isso libera os indices unicos parciais
         * da ocupacao ativa.
         */
        ocupacaoRepository.flush();

        Ocupacao novaOcupacao = new Ocupacao(
            movimentacao.getOrganizacao(),
            movimentacao.getEmbarcacao(),
            movimentacao.getVagaDestino(),
            concluidaEm,
            fimPrevisto,
            observacoes
        );

        ocupacaoRepository.save(novaOcupacao);
    }

    private void validarDestinoNaConclusao(
        Movimentacao movimentacao,
        UUID organizacaoId
    ) {
        Vaga vagaDestino = movimentacao.getVagaDestino();

        if (vagaDestino == null) {
            return;
        }

        validarVagaAtiva(vagaDestino);
        validarCompatibilidade(
            movimentacao.getEmbarcacao(),
            vagaDestino
        );

        if (movimentacao.getTipo() == TipoMovimentacao.TRANSFERENCIA) {
            ocupacaoRepository
                .findByOrganizacaoIdAndVagaIdAndStatus(
                    organizacaoId,
                    vagaDestino.getId(),
                    StatusOcupacao.ATIVA
                )
                .ifPresent(ocupacao -> {
                    throw new ConflitoDadosException(
                        "A vaga de destino possui uma ocupacao ativa"
                    );
                });
        }

        if (movimentacao.getTipo() == TipoMovimentacao.RETIRADA) {
            Ocupacao ocupacaoAtiva = buscarOcupacaoAtivaDaEmbarcacao(
                organizacaoId,
                movimentacao.getEmbarcacao().getId()
            );

            if (!Objects.equals(
                ocupacaoAtiva.getVaga().getId(),
                vagaDestino.getId()
            )) {
                throw new ConflitoDadosException(
                    "A vaga da ocupacao ativa foi alterada"
                );
            }
        }
    }

    private void validarPosicaoAindaCorrespondeOrigem(
        Movimentacao movimentacao,
        PosicaoEmbarcacao posicao
    ) {
        if (posicao.getTipo() != movimentacao.getTipoPosicaoOrigem()) {
            throw new ConflitoDadosException(
                "A posicao atual da embarcacao nao corresponde "
                    + "a origem da movimentacao"
            );
        }

        UUID vagaAtualId = posicao.getVaga() == null
            ? null
            : posicao.getVaga().getId();

        UUID vagaOrigemId = movimentacao.getVagaOrigem() == null
            ? null
            : movimentacao.getVagaOrigem().getId();

        if (!Objects.equals(vagaAtualId, vagaOrigemId)) {
            throw new ConflitoDadosException(
                "A vaga atual da embarcacao nao corresponde "
                    + "a origem da movimentacao"
            );
        }
    }

    private DestinoResolvido resolverDestino(
        TipoMovimentacao tipo,
        TipoPosicaoEmbarcacao tipoPosicaoDestino,
        UUID vagaDestinoId,
        Embarcacao embarcacao,
        UUID organizacaoId,
        UUID movimentacaoIdIgnorada
    ) {
        if (tipo == null) {
            throw new DadosInvalidosException(
                "TIPO_MOVIMENTACAO_OBRIGATORIO",
                "Tipo da movimentacao obrigatorio"
            );
        }

        if (tipoPosicaoDestino == null) {
            throw new DadosInvalidosException(
                "TIPO_DESTINO_OBRIGATORIO",
                "Tipo da posicao de destino obrigatorio"
            );
        }

        if (tipoPosicaoDestino != TipoPosicaoEmbarcacao.VAGA) {
            if (vagaDestinoId != null) {
                throw new DadosInvalidosException(
                    "VAGA_DESTINO_INDEVIDA",
                    "A vaga de destino deve ser nula quando "
                        + "o destino nao for VAGA"
                );
            }

            return new DestinoResolvido(null);
        }

        if (vagaDestinoId == null) {
            throw new DadosInvalidosException(
                "VAGA_DESTINO_OBRIGATORIA",
                "Vaga de destino obrigatoria"
            );
        }

        Vaga vaga = buscarVaga(vagaDestinoId, organizacaoId);

        validarVagaAtiva(vaga);
        validarCompatibilidade(embarcacao, vaga);
        validarReservaDestino(
            organizacaoId,
            vaga.getId(),
            movimentacaoIdIgnorada
        );

        if (tipo == TipoMovimentacao.RETIRADA) {
            Ocupacao ocupacao = buscarOcupacaoAtivaDaEmbarcacao(
                organizacaoId,
                embarcacao.getId()
            );

            if (!Objects.equals(
                ocupacao.getVaga().getId(),
                vaga.getId()
            )) {
                throw new DadosInvalidosException(
                    "DESTINO_RETIRADA_INVALIDO",
                    "A retirada deve terminar na vaga da ocupacao ativa"
                );
            }
        }

        if (tipo == TipoMovimentacao.TRANSFERENCIA) {
            ocupacaoRepository
                .findByOrganizacaoIdAndVagaIdAndStatus(
                    organizacaoId,
                    vaga.getId(),
                    StatusOcupacao.ATIVA
                )
                .ifPresent(ocupacao -> {
                    throw new ConflitoDadosException(
                        "A vaga de destino ja possui ocupacao ativa"
                    );
                });
        }

        return new DestinoResolvido(vaga);
    }

    private void validarCoerenciaOperacional(
        TipoMovimentacao tipo,
        PosicaoEmbarcacao posicao,
        DestinoResolvido destino,
        Embarcacao embarcacao,
        UUID organizacaoId
    ) {
        if (
            tipo == TipoMovimentacao.LANCAMENTO
                || tipo == TipoMovimentacao.TRANSFERENCIA
        ) {
            Ocupacao ocupacao = buscarOcupacaoAtivaDaEmbarcacao(
                organizacaoId,
                embarcacao.getId()
            );

            if (
                posicao.getTipo() != TipoPosicaoEmbarcacao.VAGA
                    || posicao.getVaga() == null
                    || !Objects.equals(
                        posicao.getVaga().getId(),
                        ocupacao.getVaga().getId()
                    )
            ) {
                throw new ConflitoDadosException(
                    "A posicao da embarcacao nao corresponde "
                        + "a ocupacao ativa"
                );
            }
        }

        if (
            tipo == TipoMovimentacao.RETIRADA
                && destino.vaga() == null
        ) {
            throw new DadosInvalidosException(
                "DESTINO_RETIRADA_INVALIDO",
                "A retirada precisa terminar em uma vaga"
            );
        }

        /*
         * O construtor da entidade valida as combinacoes finais
         * entre tipo, origem e destino.
         */
    }

    private void validarReservaDestino(
        UUID organizacaoId,
        UUID vagaDestinoId,
        UUID movimentacaoIdIgnorada
    ) {
        boolean reservada;

        if (movimentacaoIdIgnorada == null) {
            reservada = movimentacaoRepository
                .existsByOrganizacaoIdAndVagaDestinoIdAndStatusIn(
                    organizacaoId,
                    vagaDestinoId,
                    STATUS_ABERTOS
                );
        } else {
            reservada = movimentacaoRepository
                .existsByOrganizacaoIdAndVagaDestinoIdAndStatusInAndIdNot(
                    organizacaoId,
                    vagaDestinoId,
                    STATUS_ABERTOS,
                    movimentacaoIdIgnorada
                );
        }

        if (reservada) {
            throw new ConflitoDadosException(
                "A vaga de destino ja esta reservada por outra "
                    + "movimentacao aberta"
            );
        }
    }

    private void validarSemMovimentacaoAberta(
        UUID organizacaoId,
        UUID embarcacaoId
    ) {
        boolean existe = movimentacaoRepository
            .existsByOrganizacaoIdAndEmbarcacaoIdAndStatusIn(
                organizacaoId,
                embarcacaoId,
                STATUS_ABERTOS
            );

        if (existe) {
            throw new ConflitoDadosException(
                "A embarcacao ja possui uma movimentacao aberta"
            );
        }
    }

    private PosicaoEmbarcacao buscarPosicao(
        Embarcacao embarcacao,
        UUID organizacaoId
    ) {
        return posicaoRepository
            .findByOrganizacaoIdAndEmbarcacaoId(
                organizacaoId,
                embarcacao.getId()
            )
            .orElseThrow(
                () -> new RecursoNaoEncontradoException(
                    "Posicao da embarcacao nao encontrada"
                )
            );
    }

    private Movimentacao buscarMovimentacao(
        UUID id,
        UUID organizacaoId
    ) {
        if (id == null) {
            throw new DadosInvalidosException(
                "MOVIMENTACAO_OBRIGATORIA",
                "Movimentacao obrigatoria"
            );
        }

        return movimentacaoRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException(
                    "Movimentacao nao encontrada"
                )
            );
    }

    private Embarcacao buscarEmbarcacao(
        UUID id,
        UUID organizacaoId
    ) {
        if (id == null) {
            throw new DadosInvalidosException(
                "EMBARCACAO_OBRIGATORIA",
                "Embarcacao obrigatoria"
            );
        }

        return embarcacaoRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException(
                    "Embarcacao nao encontrada"
                )
            );
    }

    private Vaga buscarVaga(UUID id, UUID organizacaoId) {
        return vagaRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException(
                    "Vaga nao encontrada"
                )
            );
    }

    private Usuario buscarUsuarioAtivo(
        UUID id,
        UUID organizacaoId
    ) {
        Usuario usuario = usuarioRepository
            .findByIdAndOrganizacaoId(id, organizacaoId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException(
                    "Usuario nao encontrado"
                )
            );

        if (!usuario.isAtivo()) {
            throw new ConflitoDadosException(
                "O usuario esta inativo"
            );
        }

        return usuario;
    }

    private Usuario buscarOperadorOpcional(
        UUID id,
        UUID organizacaoId
    ) {
        if (id == null) {
            return null;
        }

        return buscarUsuarioAtivo(id, organizacaoId);
    }

    private Ocupacao buscarOcupacaoAtivaDaEmbarcacao(
        UUID organizacaoId,
        UUID embarcacaoId
    ) {
        return ocupacaoRepository
            .findByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                organizacaoId,
                embarcacaoId,
                StatusOcupacao.ATIVA
            )
            .orElseThrow(
                () -> new ConflitoDadosException(
                    "A embarcacao nao possui ocupacao ativa"
                )
            );
    }

    private void salvarMovimentacao(Movimentacao movimentacao) {
        try {
            movimentacaoRepository.save(movimentacao);
            movimentacaoRepository.flush();
        } catch (DataIntegrityViolationException excecao) {
            throw new ConflitoDadosException(
                "A movimentacao conflita com outra operacao aberta"
            );
        }
    }

    private void validarFiltrosListagem(
        StatusMovimentacao status,
        TipoMovimentacao tipo,
        UUID embarcacaoId,
        Instant inicio,
        Instant fim
    ) {
        if ((inicio == null) != (fim == null)) {
            throw new DadosInvalidosException(
                "PERIODO_INCOMPLETO",
                "Inicio e fim do periodo devem ser informados juntos"
            );
        }

        if (inicio != null && !fim.isAfter(inicio)) {
            throw new DadosInvalidosException(
                "PERIODO_INVALIDO",
                "O fim do periodo deve ser posterior ao inicio"
            );
        }

        int quantidadeFiltros = 0;

        if (status != null) {
            quantidadeFiltros++;
        }
        if (tipo != null) {
            quantidadeFiltros++;
        }
        if (embarcacaoId != null) {
            quantidadeFiltros++;
        }
        if (inicio != null) {
            quantidadeFiltros++;
        }

        if (quantidadeFiltros > 1) {
            throw new DadosInvalidosException(
                "FILTROS_INCOMPATIVEIS",
                "Use apenas um filtro por vez"
            );
        }
    }

    private void validarAgendamento(Instant agendadaPara) {
        if (agendadaPara == null) {
            throw new DadosInvalidosException(
                "AGENDAMENTO_OBRIGATORIO",
                "Data de agendamento obrigatoria"
            );
        }

        Instant limitePassado = Instant.now().minus(TOLERANCIA_RELOGIO);

        if (agendadaPara.isBefore(limitePassado)) {
            throw new DadosInvalidosException(
                "AGENDAMENTO_PASSADO",
                "A movimentacao nao pode ser agendada no passado"
            );
        }
    }

    private void validarDataOperacional(
        Instant data,
        Instant limiteInferior,
        String codigo,
        String descricao
    ) {
        if (data == null) {
            throw new DadosInvalidosException(
                codigo,
                descricao + " e obrigatoria"
            );
        }

        if (
            limiteInferior != null
                && data.isBefore(limiteInferior)
        ) {
            throw new DadosInvalidosException(
                codigo,
                descricao + " nao pode ser anterior ao evento precedente"
            );
        }

        Instant limiteFuturo = Instant.now().plus(TOLERANCIA_RELOGIO);

        if (data.isAfter(limiteFuturo)) {
            throw new DadosInvalidosException(
                codigo,
                descricao + " nao pode estar no futuro"
            );
        }
    }

    private void validarEmbarcacaoAtiva(Embarcacao embarcacao) {
        if (!embarcacao.isAtiva()) {
            throw new ConflitoDadosException(
                "Nao e possivel movimentar uma embarcacao inativa"
            );
        }
    }

    private void validarVagaAtiva(Vaga vaga) {
        if (!vaga.isAtiva()) {
            throw new ConflitoDadosException(
                "Nao e possivel usar uma vaga inativa"
            );
        }
    }

    private void validarCompatibilidade(
        Embarcacao embarcacao,
        Vaga vaga
    ) {
        validarLimite(
            embarcacao.getComprimentoTotalMetros(),
            vaga.getComprimentoMaximoMetros(),
            "COMPRIMENTO_EXCEDE_LIMITE_VAGA",
            "O comprimento da embarcacao excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getBocaMetros(),
            vaga.getBocaMaximaMetros(),
            "BOCA_EXCEDE_LIMITE_VAGA",
            "A boca da embarcacao excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getCaladoMetros(),
            vaga.getCaladoMaximoMetros(),
            "CALADO_EXCEDE_LIMITE_VAGA",
            "O calado da embarcacao excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getAlturaTotalMetros(),
            vaga.getAlturaMaximaMetros(),
            "ALTURA_EXCEDE_LIMITE_VAGA",
            "A altura da embarcacao excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getPesoKg(),
            vaga.getPesoMaximoKg(),
            "PESO_EXCEDE_LIMITE_VAGA",
            "O peso da embarcacao excede o limite da vaga"
        );
    }

    private void validarLimite(
        BigDecimal medida,
        BigDecimal limite,
        String codigo,
        String mensagem
    ) {
        if (
            medida != null
                && limite != null
                && medida.compareTo(limite) > 0
        ) {
            throw new DadosInvalidosException(codigo, mensagem);
        }
    }

    private Map<String, Object> criarSnapshotAtualizavel(
        Movimentacao movimentacao
    ) {
        Map<String, Object> dados = new LinkedHashMap<>();

        dados.put(
            "prioridade",
            nomeEnum(movimentacao.getPrioridade())
        );
        dados.put(
            "tipoPosicaoDestino",
            nomeEnum(movimentacao.getTipoPosicaoDestino())
        );
        dados.put(
            "vagaDestinoId",
            uuidTexto(movimentacao.getVagaDestino())
        );
        dados.put(
            "descricaoDestino",
            movimentacao.getDescricaoDestino()
        );
        dados.put(
            "operadorResponsavelId",
            movimentacao.getOperadorResponsavel() == null
                ? null
                : movimentacao
                    .getOperadorResponsavel()
                    .getId()
                    .toString()
        );
        dados.put(
            "observacoes",
            movimentacao.getObservacoes()
        );

        return dados;
    }

    private String nomeEnum(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private String uuidTexto(Vaga vaga) {
        return vaga == null ? null : vaga.getId().toString();
    }

    private String criarObservacaoTransferencia(
        String observacaoAnterior,
        UUID movimentacaoId
    ) {
        String complemento = "Transferencia concluida pela movimentacao "
            + movimentacaoId;

        String resultado;

        if (
            observacaoAnterior == null
                || observacaoAnterior.isBlank()
        ) {
            resultado = complemento;
        } else {
            resultado = observacaoAnterior.trim()
                + "\n"
                + complemento;
        }

        if (resultado.length() > 2000) {
            return resultado.substring(0, 2000);
        }

        return resultado;
    }

    private void executarRegraDominio(Runnable operacao) {
        try {
            operacao.run();
        } catch (
            IllegalArgumentException
                | IllegalStateException
                | NullPointerException excecao
        ) {
            throw new DadosInvalidosException(
                "MOVIMENTACAO_INVALIDA",
                excecao.getMessage()
            );
        }
    }

    private void validarRequest(Object request) {
        if (request == null) {
            throw new DadosInvalidosException(
                "CORPO_REQUISICAO_OBRIGATORIO",
                "Corpo da requisicao obrigatorio"
            );
        }
    }

    private UsuarioAutenticado obterUsuarioAutenticado() {
        return leitorTokenJwt.obterUsuarioAutenticado();
    }

    private record DestinoResolvido(Vaga vaga) {
    }
}
