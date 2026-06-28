package br.com.caisora.embarcacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.api.AlterarStatusEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.AtualizarEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.CriarEmbarcacaoRequest;
import br.com.caisora.embarcacao.api.EmbarcacaoResponse;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbarcacaoService {

    private final EmbarcacaoRepository embarcacaoRepository;
    private final ClienteRepository clienteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final EmbarcacaoMapper embarcacaoMapper;
    private final LeitorTokenJwt leitorTokenJwt;

    public EmbarcacaoService(
            EmbarcacaoRepository embarcacaoRepository,
            ClienteRepository clienteRepository,
            OrganizacaoRepository organizacaoRepository,
            EmbarcacaoMapper embarcacaoMapper,
            LeitorTokenJwt leitorTokenJwt
    ) {
        this.embarcacaoRepository = embarcacaoRepository;
        this.clienteRepository = clienteRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.embarcacaoMapper = embarcacaoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public EmbarcacaoResponse criar(
            CriarEmbarcacaoRequest request
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Organizacao organizacao =
                buscarOrganizacao(organizacaoId);

        Cliente proprietario = buscarProprietario(
                organizacaoId,
                request.proprietarioId()
        );

        validarProprietarioParaVinculo(
                proprietario,
                null
        );

        String numeroInscricao =
                normalizarIdentificador(
                        request.numeroInscricao()
                );

        String numeroCasco =
                normalizarIdentificador(
                        request.numeroCasco()
                );

        String codigoPais =
                normalizarCodigoPais(
                        request.codigoPaisBandeira()
                );

        validarDados(
                request.tipo(),
                request.tipoPropulsao(),
                request.anoFabricacao(),
                request.comprimentoTotalMetros(),
                request.bocaMetros(),
                request.caladoMetros(),
                request.pontalMetros(),
                request.alturaTotalMetros(),
                request.pesoKg(),
                request.capacidadePessoas(),
                codigoPais
        );

        validarIdentificadoresDisponiveis(
                organizacaoId,
                numeroInscricao,
                numeroCasco
        );

        Embarcacao embarcacao = new Embarcacao(
                organizacao,
                proprietario,
                normalizarTextoOpcional(request.nome()),
                request.tipo(),
                normalizarTextoOpcional(request.fabricante()),
                normalizarTextoOpcional(request.modelo()),
                request.anoFabricacao(),
                numeroInscricao,
                numeroCasco,
                normalizarTextoOpcional(
                        request.portoInscricao()
                ),
                codigoPais,
                request.comprimentoTotalMetros(),
                request.bocaMetros(),
                request.caladoMetros(),
                request.pontalMetros(),
                request.alturaTotalMetros(),
                request.pesoKg(),
                request.capacidadePessoas(),
                request.tipoPropulsao(),
                normalizarTextoOpcional(
                        request.corPredominante()
                ),
                normalizarTextoOpcional(
                        request.observacoes()
                )
        );

        Embarcacao salva =
                embarcacaoRepository.save(embarcacao);

        return embarcacaoMapper.paraResponse(salva);
    }

    @Transactional(readOnly = true)
    public Page<EmbarcacaoResponse> listar(
            Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return embarcacaoRepository
                .findAllByOrganizacaoId(
                        organizacaoId,
                        paginacao
                )
                .map(embarcacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmbarcacaoResponse> listarPorStatus(
            boolean ativa,
            Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return embarcacaoRepository
                .findAllByOrganizacaoIdAndAtiva(
                        organizacaoId,
                        ativa,
                        paginacao
                )
                .map(embarcacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmbarcacaoResponse> buscarPorNome(
            String nome,
            Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return embarcacaoRepository
                .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                        organizacaoId,
                        nome.trim(),
                        paginacao
                )
                .map(embarcacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmbarcacaoResponse> listarPorProprietario(
            UUID proprietarioId,
            Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        buscarProprietario(
                organizacaoId,
                proprietarioId
        );

        return embarcacaoRepository
                .findAllByOrganizacaoIdAndProprietarioId(
                        organizacaoId,
                        proprietarioId,
                        paginacao
                )
                .map(embarcacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmbarcacaoResponse> listarPorTipo(
            TipoEmbarcacao tipo,
            Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return embarcacaoRepository
                .findAllByOrganizacaoIdAndTipo(
                        organizacaoId,
                        tipo,
                        paginacao
                )
                .map(embarcacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public EmbarcacaoResponse buscarPorId(UUID id) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Embarcacao embarcacao =
                buscarEntidadePorId(
                        organizacaoId,
                        id
                );

        return embarcacaoMapper.paraResponse(embarcacao);
    }

    @Transactional
    public EmbarcacaoResponse atualizar(
            UUID id,
            AtualizarEmbarcacaoRequest request
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Embarcacao embarcacao =
                buscarEntidadePorId(
                        organizacaoId,
                        id
                );

        Cliente proprietario = buscarProprietario(
                organizacaoId,
                request.proprietarioId()
        );

        validarProprietarioParaVinculo(
                proprietario,
                embarcacao.getProprietario().getId()
        );

        String numeroInscricao =
                normalizarIdentificador(
                        request.numeroInscricao()
                );

        String numeroCasco =
                normalizarIdentificador(
                        request.numeroCasco()
                );

        String codigoPais =
                normalizarCodigoPais(
                        request.codigoPaisBandeira()
                );

        validarDados(
                request.tipo(),
                request.tipoPropulsao(),
                request.anoFabricacao(),
                request.comprimentoTotalMetros(),
                request.bocaMetros(),
                request.caladoMetros(),
                request.pontalMetros(),
                request.alturaTotalMetros(),
                request.pesoKg(),
                request.capacidadePessoas(),
                codigoPais
        );

        validarIdentificadoresDisponiveisNaAtualizacao(
                organizacaoId,
                numeroInscricao,
                numeroCasco,
                id
        );

        embarcacao.atualizarDados(
                proprietario,
                normalizarTextoOpcional(request.nome()),
                request.tipo(),
                normalizarTextoOpcional(request.fabricante()),
                normalizarTextoOpcional(request.modelo()),
                request.anoFabricacao(),
                numeroInscricao,
                numeroCasco,
                normalizarTextoOpcional(
                        request.portoInscricao()
                ),
                codigoPais,
                request.comprimentoTotalMetros(),
                request.bocaMetros(),
                request.caladoMetros(),
                request.pontalMetros(),
                request.alturaTotalMetros(),
                request.pesoKg(),
                request.capacidadePessoas(),
                request.tipoPropulsao(),
                normalizarTextoOpcional(
                        request.corPredominante()
                ),
                normalizarTextoOpcional(
                        request.observacoes()
                )
        );

        Embarcacao salva =
                embarcacaoRepository.save(embarcacao);

        return embarcacaoMapper.paraResponse(salva);
    }

    @Transactional
    public EmbarcacaoResponse alterarStatus(
            UUID id,
            AlterarStatusEmbarcacaoRequest request
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Embarcacao embarcacao =
                buscarEntidadePorId(
                        organizacaoId,
                        id
                );

        if (Boolean.TRUE.equals(request.ativa())) {
            embarcacao.ativar();
        } else {
            embarcacao.inativar();
        }

        Embarcacao salva =
                embarcacaoRepository.save(embarcacao);

        return embarcacaoMapper.paraResponse(salva);
    }

    private Embarcacao buscarEntidadePorId(
            UUID organizacaoId,
            UUID embarcacaoId
    ) {
        return embarcacaoRepository
                .findByIdAndOrganizacaoId(
                        embarcacaoId,
                        organizacaoId
                )
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Embarcacao nao encontrada"
                        )
                );
    }

    private Cliente buscarProprietario(
            UUID organizacaoId,
            UUID proprietarioId
    ) {
        if (proprietarioId == null) {
            throw new DadosInvalidosException(
                    "PROPRIETARIO_OBRIGATORIO",
                    "Proprietario obrigatorio"
            );
        }

        return clienteRepository
                .findByIdAndOrganizacaoId(
                        proprietarioId,
                        organizacaoId
                )
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Proprietario nao encontrado"
                        )
                );
    }

    private Organizacao buscarOrganizacao(
            UUID organizacaoId
    ) {
        return organizacaoRepository
                .findById(organizacaoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Organizacao nao encontrada"
                        )
                );
    }

    private void validarProprietarioParaVinculo(
            Cliente proprietario,
            UUID proprietarioAtualId
    ) {
        boolean novoVinculo =
                proprietarioAtualId == null
                        || !proprietario.getId()
                        .equals(proprietarioAtualId);

        if (novoVinculo && !proprietario.isAtivo()) {
            throw new DadosInvalidosException(
                    "PROPRIETARIO_INATIVO",
                    "Nao e permitido vincular uma embarcacao "
                            + "a um proprietario inativo"
            );
        }
    }

    private void validarIdentificadoresDisponiveis(
            UUID organizacaoId,
            String numeroInscricao,
            String numeroCasco
    ) {
        if (
                numeroInscricao != null
                && embarcacaoRepository
                .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCase(
                        organizacaoId,
                        numeroInscricao
                )
        ) {
            throw new ConflitoDadosException(
                    "Ja existe uma embarcacao com este "
                            + "numero de inscricao na organizacao"
            );
        }

        if (
                numeroCasco != null
                && embarcacaoRepository
                .existsByOrganizacaoIdAndNumeroCascoIgnoreCase(
                        organizacaoId,
                        numeroCasco
                )
        ) {
            throw new ConflitoDadosException(
                    "Ja existe uma embarcacao com este "
                            + "numero de casco na organizacao"
            );
        }
    }

    private void validarIdentificadoresDisponiveisNaAtualizacao(
            UUID organizacaoId,
            String numeroInscricao,
            String numeroCasco,
            UUID embarcacaoId
    ) {
        if (
                numeroInscricao != null
                && embarcacaoRepository
                .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
                        organizacaoId,
                        numeroInscricao,
                        embarcacaoId
                )
        ) {
            throw new ConflitoDadosException(
                    "Ja existe uma embarcacao com este "
                            + "numero de inscricao na organizacao"
            );
        }

        if (
                numeroCasco != null
                && embarcacaoRepository
                .existsByOrganizacaoIdAndNumeroCascoIgnoreCaseAndIdNot(
                        organizacaoId,
                        numeroCasco,
                        embarcacaoId
                )
        ) {
            throw new ConflitoDadosException(
                    "Ja existe uma embarcacao com este "
                            + "numero de casco na organizacao"
            );
        }
    }

    private void validarDados(
            TipoEmbarcacao tipo,
            TipoPropulsao tipoPropulsao,
            Integer anoFabricacao,
            BigDecimal comprimento,
            BigDecimal boca,
            BigDecimal calado,
            BigDecimal pontal,
            BigDecimal altura,
            BigDecimal peso,
            Integer capacidadePessoas,
            String codigoPais
    ) {
        if (tipo == null) {
            throw new DadosInvalidosException(
                    "TIPO_EMBARCACAO_OBRIGATORIO",
                    "Tipo de embarcacao obrigatorio"
            );
        }

        if (tipoPropulsao == null) {
            throw new DadosInvalidosException(
                    "TIPO_PROPULSAO_OBRIGATORIO",
                    "Tipo de propulsao obrigatorio"
            );
        }

        validarMedidaObrigatoria(
                comprimento,
                "COMPRIMENTO_INVALIDO",
                "Comprimento total deve ser maior que zero"
        );

        validarMedidaObrigatoria(
                boca,
                "BOCA_INVALIDA",
                "Boca deve ser maior que zero"
        );

        validarMedidaOpcional(
                calado,
                "CALADO_INVALIDO",
                "Calado deve ser maior que zero"
        );

        validarMedidaOpcional(
                pontal,
                "PONTAL_INVALIDO",
                "Pontal deve ser maior que zero"
        );

        validarMedidaOpcional(
                altura,
                "ALTURA_TOTAL_INVALIDA",
                "Altura total deve ser maior que zero"
        );

        validarMedidaOpcional(
                peso,
                "PESO_INVALIDO",
                "Peso deve ser maior que zero"
        );

        if (
                capacidadePessoas != null
                && capacidadePessoas <= 0
        ) {
            throw new DadosInvalidosException(
                    "CAPACIDADE_PESSOAS_INVALIDA",
                    "Capacidade de pessoas deve ser maior que zero"
            );
        }

        int anoMaximo = Year.now().getValue() + 1;

        if (
                anoFabricacao != null
                && (
                    anoFabricacao < 1800
                    || anoFabricacao > anoMaximo
                )
        ) {
            throw new DadosInvalidosException(
                    "ANO_FABRICACAO_INVALIDO",
                    "Ano de fabricacao invalido"
            );
        }

        if (
                codigoPais == null
                || !codigoPais.matches("[A-Z]{2}")
        ) {
            throw new DadosInvalidosException(
                    "PAIS_BANDEIRA_INVALIDO",
                    "Codigo do pais da bandeira deve possuir "
                            + "duas letras"
            );
        }
    }

    private void validarMedidaObrigatoria(
            BigDecimal valor,
            String codigo,
            String mensagem
    ) {
        if (
                valor == null
                || valor.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new DadosInvalidosException(
                    codigo,
                    mensagem
            );
        }
    }

    private void validarMedidaOpcional(
            BigDecimal valor,
            String codigo,
            String mensagem
    ) {
        if (
                valor != null
                && valor.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new DadosInvalidosException(
                    codigo,
                    mensagem
            );
        }
    }

    private UUID obterOrganizacaoAutenticada() {
        return leitorTokenJwt
                .obterUsuarioAutenticado()
                .organizacaoId();
    }

    private String normalizarIdentificador(
            String identificador
    ) {
        if (
                identificador == null
                || identificador.isBlank()
        ) {
            return null;
        }

        return identificador
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizarCodigoPais(
            String codigo
    ) {
        if (codigo == null || codigo.isBlank()) {
            return "BR";
        }

        return codigo
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizarTextoOpcional(
            String texto
    ) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}