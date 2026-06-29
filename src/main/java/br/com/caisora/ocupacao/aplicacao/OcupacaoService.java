package br.com.caisora.ocupacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.ocupacao.api.AtualizarOcupacaoRequest;
import br.com.caisora.ocupacao.api.CriarOcupacaoRequest;
import br.com.caisora.ocupacao.api.EncerrarOcupacaoRequest;
import br.com.caisora.ocupacao.api.OcupacaoResponse;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class OcupacaoService {

    private static final Duration TOLERANCIA_RELOGIO =
        Duration.ofMinutes(1);

    private final OcupacaoRepository ocupacaoRepository;

    private final EmbarcacaoRepository embarcacaoRepository;

    private final VagaRepository vagaRepository;

    private final OcupacaoMapper ocupacaoMapper;

    private final LeitorTokenJwt leitorTokenJwt;

    public OcupacaoService(
        OcupacaoRepository ocupacaoRepository,
        EmbarcacaoRepository embarcacaoRepository,
        VagaRepository vagaRepository,
        OcupacaoMapper ocupacaoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.ocupacaoRepository =
            ocupacaoRepository;

        this.embarcacaoRepository =
            embarcacaoRepository;

        this.vagaRepository =
            vagaRepository;

        this.ocupacaoMapper =
            ocupacaoMapper;

        this.leitorTokenJwt =
            leitorTokenJwt;
    }

    @Transactional
    public OcupacaoResponse criar(
        CriarOcupacaoRequest request
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        validarDatasCriacao(
            request.inicioEm(),
            request.fimPrevistoEm()
        );

        Embarcacao embarcacao =
            buscarEmbarcacao(
                request.embarcacaoId(),
                organizacaoId
            );

        Vaga vaga =
            buscarVaga(
                request.vagaId(),
                organizacaoId
            );

        validarEmbarcacaoAtiva(embarcacao);
        validarVagaAtiva(vaga);

        validarDisponibilidade(
            organizacaoId,
            embarcacao.getId(),
            vaga.getId()
        );

        validarCompatibilidade(
            embarcacao,
            vaga
        );

        Ocupacao ocupacao =
            new Ocupacao(
                embarcacao.getOrganizacao(),
                embarcacao,
                vaga,
                request.inicioEm(),
                request.fimPrevistoEm(),
                normalizarTextoOpcional(
                    request.observacoes()
                )
            );

        Ocupacao ocupacaoSalva =
            ocupacaoRepository.save(ocupacao);

        return ocupacaoMapper.paraResponse(
            ocupacaoSalva
        );
    }

    @Transactional(readOnly = true)
    public Page<OcupacaoResponse> listar(
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return ocupacaoRepository
            .findAllByOrganizacaoId(
                organizacaoId,
                paginacao
            )
            .map(ocupacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<OcupacaoResponse> listarPorStatus(
        StatusOcupacao status,
        Pageable paginacao
    ) {
        if (status == null) {
            throw new DadosInvalidosException(
                "STATUS_OCUPACAO_OBRIGATORIO",
                "Status da ocupacao obrigatorio"
            );
        }

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return ocupacaoRepository
            .findAllByOrganizacaoIdAndStatus(
                organizacaoId,
                status,
                paginacao
            )
            .map(ocupacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<OcupacaoResponse> listarPorEmbarcacao(
        UUID embarcacaoId,
        Pageable paginacao
    ) {
        validarIdentificador(
            embarcacaoId,
            "EMBARCACAO_OBRIGATORIA",
            "Embarcacao obrigatoria"
        );

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return ocupacaoRepository
            .findAllByOrganizacaoIdAndEmbarcacaoId(
                organizacaoId,
                embarcacaoId,
                paginacao
            )
            .map(ocupacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<OcupacaoResponse> listarPorVaga(
        UUID vagaId,
        Pageable paginacao
    ) {
        validarIdentificador(
            vagaId,
            "VAGA_OBRIGATORIA",
            "Vaga obrigatoria"
        );

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return ocupacaoRepository
            .findAllByOrganizacaoIdAndVagaId(
                organizacaoId,
                vagaId,
                paginacao
            )
            .map(ocupacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public OcupacaoResponse buscarPorId(
        UUID id
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Ocupacao ocupacao =
            buscarEntidadePorId(
                id,
                organizacaoId
            );

        return ocupacaoMapper.paraResponse(
            ocupacao
        );
    }

    @Transactional
    public OcupacaoResponse atualizar(
        UUID id,
        AtualizarOcupacaoRequest request
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Ocupacao ocupacao =
            buscarEntidadePorId(
                id,
                organizacaoId
            );

        validarOcupacaoAtiva(ocupacao);

        validarFimPrevisto(
            ocupacao.getInicioEm(),
            request.fimPrevistoEm()
        );

        ocupacao.atualizarDados(
            request.fimPrevistoEm(),
            normalizarTextoOpcional(
                request.observacoes()
            )
        );

        Ocupacao ocupacaoSalva =
            ocupacaoRepository.save(ocupacao);

        return ocupacaoMapper.paraResponse(
            ocupacaoSalva
        );
    }

    @Transactional
    public OcupacaoResponse encerrar(
        UUID id,
        EncerrarOcupacaoRequest request
    ) {
        if (
            request == null
                || request.encerradaEm() == null
        ) {
            throw new DadosInvalidosException(
                "DATA_ENCERRAMENTO_OBRIGATORIA",
                "Data de encerramento obrigatoria"
            );
        }

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Ocupacao ocupacao =
            buscarEntidadePorId(
                id,
                organizacaoId
            );

        validarOcupacaoAtiva(ocupacao);

        validarDataEncerramento(
            ocupacao.getInicioEm(),
            request.encerradaEm()
        );

        ocupacao.encerrar(
            request.encerradaEm()
        );

        Ocupacao ocupacaoSalva =
            ocupacaoRepository.save(ocupacao);

        return ocupacaoMapper.paraResponse(
            ocupacaoSalva
        );
    }

    private Ocupacao buscarEntidadePorId(
        UUID ocupacaoId,
        UUID organizacaoId
    ) {
        return ocupacaoRepository
            .findByIdAndOrganizacaoId(
                ocupacaoId,
                organizacaoId
            )
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Ocupacao nao encontrada"
                    )
            );
    }

    private Embarcacao buscarEmbarcacao(
        UUID embarcacaoId,
        UUID organizacaoId
    ) {
        validarIdentificador(
            embarcacaoId,
            "EMBARCACAO_OBRIGATORIA",
            "Embarcacao obrigatoria"
        );

        return embarcacaoRepository
            .findByIdAndOrganizacaoId(
                embarcacaoId,
                organizacaoId
            )
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Embarcacao nao encontrada"
                    )
            );
    }

    private Vaga buscarVaga(
        UUID vagaId,
        UUID organizacaoId
    ) {
        validarIdentificador(
            vagaId,
            "VAGA_OBRIGATORIA",
            "Vaga obrigatoria"
        );

        return vagaRepository
            .findByIdAndOrganizacaoId(
                vagaId,
                organizacaoId
            )
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Vaga nao encontrada"
                    )
            );
    }

    private void validarDisponibilidade(
        UUID organizacaoId,
        UUID embarcacaoId,
        UUID vagaId
    ) {
        boolean embarcacaoOcupada =
            ocupacaoRepository
                .existsByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                    organizacaoId,
                    embarcacaoId,
                    StatusOcupacao.ATIVA
                );

        if (embarcacaoOcupada) {
            throw new ConflitoDadosException(
                "A embarcacao ja possui uma "
                    + "ocupacao ativa"
            );
        }

        boolean vagaOcupada =
            ocupacaoRepository
                .existsByOrganizacaoIdAndVagaIdAndStatus(
                    organizacaoId,
                    vagaId,
                    StatusOcupacao.ATIVA
                );

        if (vagaOcupada) {
            throw new ConflitoDadosException(
                "A vaga ja possui uma "
                    + "ocupacao ativa"
            );
        }
    }

    private void validarEmbarcacaoAtiva(
        Embarcacao embarcacao
    ) {
        if (!embarcacao.isAtiva()) {
            throw new ConflitoDadosException(
                "Nao e possivel ocupar uma vaga "
                    + "com uma embarcacao inativa"
            );
        }
    }

    private void validarVagaAtiva(
        Vaga vaga
    ) {
        if (!vaga.isAtiva()) {
            throw new ConflitoDadosException(
                "Nao e possivel criar uma ocupacao "
                    + "em uma vaga inativa"
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
            "O comprimento da embarcacao "
                + "excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getBocaMetros(),
            vaga.getBocaMaximaMetros(),
            "BOCA_EXCEDE_LIMITE_VAGA",
            "A boca da embarcacao "
                + "excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getCaladoMetros(),
            vaga.getCaladoMaximoMetros(),
            "CALADO_EXCEDE_LIMITE_VAGA",
            "O calado da embarcacao "
                + "excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getAlturaTotalMetros(),
            vaga.getAlturaMaximaMetros(),
            "ALTURA_EXCEDE_LIMITE_VAGA",
            "A altura da embarcacao "
                + "excede o limite da vaga"
        );

        validarLimite(
            embarcacao.getPesoKg(),
            vaga.getPesoMaximoKg(),
            "PESO_EXCEDE_LIMITE_VAGA",
            "O peso da embarcacao "
                + "excede o limite da vaga"
        );
    }

    private void validarLimite(
        BigDecimal medidaEmbarcacao,
        BigDecimal limiteVaga,
        String codigo,
        String mensagem
    ) {
        if (
            medidaEmbarcacao != null
                && limiteVaga != null
                && medidaEmbarcacao.compareTo(
                    limiteVaga
                ) > 0
        ) {
            throw new DadosInvalidosException(
                codigo,
                mensagem
            );
        }
    }

    private void validarDatasCriacao(
        Instant inicioEm,
        Instant fimPrevistoEm
    ) {
        if (inicioEm == null) {
            throw new DadosInvalidosException(
                "INICIO_OCUPACAO_OBRIGATORIO",
                "Inicio da ocupacao obrigatorio"
            );
        }

        Instant limiteFuturo =
            Instant.now().plus(
                TOLERANCIA_RELOGIO
            );

        if (inicioEm.isAfter(limiteFuturo)) {
            throw new DadosInvalidosException(
                "INICIO_OCUPACAO_FUTURO",
                "O inicio da ocupacao nao pode "
                    + "estar no futuro"
            );
        }

        validarFimPrevisto(
            inicioEm,
            fimPrevistoEm
        );
    }

    private void validarFimPrevisto(
        Instant inicioEm,
        Instant fimPrevistoEm
    ) {
        if (
            fimPrevistoEm != null
                && !fimPrevistoEm.isAfter(
                    inicioEm
                )
        ) {
            throw new DadosInvalidosException(
                "FIM_PREVISTO_INVALIDO",
                "O fim previsto deve ser posterior "
                    + "ao inicio da ocupacao"
            );
        }
    }

    private void validarDataEncerramento(
        Instant inicioEm,
        Instant encerradaEm
    ) {
        if (encerradaEm.isBefore(inicioEm)) {
            throw new DadosInvalidosException(
                "ENCERRAMENTO_ANTERIOR_INICIO",
                "O encerramento nao pode ser "
                    + "anterior ao inicio da ocupacao"
            );
        }

        Instant limiteFuturo =
            Instant.now().plus(
                TOLERANCIA_RELOGIO
            );

        if (encerradaEm.isAfter(limiteFuturo)) {
            throw new DadosInvalidosException(
                "ENCERRAMENTO_FUTURO",
                "O encerramento nao pode "
                    + "estar no futuro"
            );
        }
    }

    private void validarOcupacaoAtiva(
        Ocupacao ocupacao
    ) {
        if (!ocupacao.estaAtiva()) {
            throw new ConflitoDadosException(
                "A ocupacao ja esta encerrada"
            );
        }
    }

    private void validarIdentificador(
        UUID id,
        String codigo,
        String mensagem
    ) {
        if (id == null) {
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

    private String normalizarTextoOpcional(
        String texto
    ) {
        if (
            texto == null
                || texto.isBlank()
        ) {
            return null;
        }

        return texto.trim();
    }
}
