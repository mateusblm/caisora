package br.com.caisora.vaga.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.vaga.api.AlterarStatusVagaRequest;
import br.com.caisora.vaga.api.AtualizarVagaRequest;
import br.com.caisora.vaga.api.CriarVagaRequest;
import br.com.caisora.vaga.api.VagaResponse;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;

    private final OrganizacaoRepository organizacaoRepository;

    private final VagaMapper vagaMapper;

    private final LeitorTokenJwt leitorTokenJwt;

    public VagaService(
        VagaRepository vagaRepository,
        OrganizacaoRepository organizacaoRepository,
        VagaMapper vagaMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.vagaRepository = vagaRepository;
        this.organizacaoRepository =
            organizacaoRepository;
        this.vagaMapper = vagaMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public VagaResponse criar(
        CriarVagaRequest request
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Organizacao organizacao =
            buscarOrganizacao(organizacaoId);

        String codigo =
            normalizarCodigo(request.codigo());

        validarDados(
            request.tipo(),
            request.comprimentoMaximoMetros(),
            request.bocaMaximaMetros(),
            request.caladoMaximoMetros(),
            request.alturaMaximaMetros(),
            request.pesoMaximoKg(),
            request.possuiAgua(),
            request.possuiEnergia()
        );

        validarCodigoDisponivel(
            organizacaoId,
            codigo
        );

        Vaga vaga = new Vaga(
            organizacao,
            codigo,
            request.tipo(),
            normalizarTextoOpcional(
                request.setor()
            ),
            normalizarTextoOpcional(
                request.localizacao()
            ),
            request.comprimentoMaximoMetros(),
            request.bocaMaximaMetros(),
            request.caladoMaximoMetros(),
            request.alturaMaximaMetros(),
            request.pesoMaximoKg(),
            Boolean.TRUE.equals(
                request.possuiAgua()
            ),
            Boolean.TRUE.equals(
                request.possuiEnergia()
            ),
            normalizarTextoOpcional(
                request.observacoes()
            )
        );

        Vaga vagaSalva =
            vagaRepository.save(vaga);

        return vagaMapper.paraResponse(
            vagaSalva
        );
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> listar(
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return vagaRepository
            .findAllByOrganizacaoId(
                organizacaoId,
                paginacao
            )
            .map(vagaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> listarPorStatus(
        boolean ativa,
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return vagaRepository
            .findAllByOrganizacaoIdAndAtiva(
                organizacaoId,
                ativa,
                paginacao
            )
            .map(vagaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> buscarPorCodigo(
        String codigo,
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        String termo =
            normalizarTermoBusca(
                codigo,
                "CODIGO_BUSCA_OBRIGATORIO",
                "Codigo para busca obrigatorio"
            );

        return vagaRepository
            .findAllByOrganizacaoIdAndCodigoContainingIgnoreCase(
                organizacaoId,
                termo,
                paginacao
            )
            .map(vagaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> listarPorTipo(
        TipoVaga tipo,
        Pageable paginacao
    ) {
        if (tipo == null) {
            throw new DadosInvalidosException(
                "TIPO_VAGA_OBRIGATORIO",
                "Tipo de vaga obrigatorio"
            );
        }

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        return vagaRepository
            .findAllByOrganizacaoIdAndTipo(
                organizacaoId,
                tipo,
                paginacao
            )
            .map(vagaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> buscarPorSetor(
        String setor,
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        String termo =
            normalizarTermoBusca(
                setor,
                "SETOR_BUSCA_OBRIGATORIO",
                "Setor para busca obrigatorio"
            );

        return vagaRepository
            .findAllByOrganizacaoIdAndSetorContainingIgnoreCase(
                organizacaoId,
                termo,
                paginacao
            )
            .map(vagaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public VagaResponse buscarPorId(
        UUID id
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Vaga vaga = buscarEntidadePorId(
            organizacaoId,
            id
        );

        return vagaMapper.paraResponse(vaga);
    }

    @Transactional
    public VagaResponse atualizar(
        UUID id,
        AtualizarVagaRequest request
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Vaga vaga = buscarEntidadePorId(
            organizacaoId,
            id
        );

        String codigo =
            normalizarCodigo(request.codigo());

        validarDados(
            request.tipo(),
            request.comprimentoMaximoMetros(),
            request.bocaMaximaMetros(),
            request.caladoMaximoMetros(),
            request.alturaMaximaMetros(),
            request.pesoMaximoKg(),
            request.possuiAgua(),
            request.possuiEnergia()
        );

        validarCodigoDisponivelNaAtualizacao(
            organizacaoId,
            codigo,
            id
        );

        vaga.atualizarDados(
            codigo,
            request.tipo(),
            normalizarTextoOpcional(
                request.setor()
            ),
            normalizarTextoOpcional(
                request.localizacao()
            ),
            request.comprimentoMaximoMetros(),
            request.bocaMaximaMetros(),
            request.caladoMaximoMetros(),
            request.alturaMaximaMetros(),
            request.pesoMaximoKg(),
            Boolean.TRUE.equals(
                request.possuiAgua()
            ),
            Boolean.TRUE.equals(
                request.possuiEnergia()
            ),
            normalizarTextoOpcional(
                request.observacoes()
            )
        );

        Vaga vagaSalva =
            vagaRepository.save(vaga);

        return vagaMapper.paraResponse(
            vagaSalva
        );
    }

    @Transactional
    public VagaResponse alterarStatus(
        UUID id,
        AlterarStatusVagaRequest request
    ) {
        if (request.ativa() == null) {
            throw new DadosInvalidosException(
                "STATUS_VAGA_OBRIGATORIO",
                "Status da vaga obrigatorio"
            );
        }

        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Vaga vaga = buscarEntidadePorId(
            organizacaoId,
            id
        );

        if (Boolean.TRUE.equals(
            request.ativa()
        )) {
            vaga.ativar();
        } else {
            vaga.inativar();
        }

        Vaga vagaSalva =
            vagaRepository.save(vaga);

        return vagaMapper.paraResponse(
            vagaSalva
        );
    }

    private Vaga buscarEntidadePorId(
        UUID organizacaoId,
        UUID vagaId
    ) {
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

    private Organizacao buscarOrganizacao(
        UUID organizacaoId
    ) {
        return organizacaoRepository
            .findById(organizacaoId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Organizacao nao encontrada"
                    )
            );
    }

    private void validarCodigoDisponivel(
        UUID organizacaoId,
        String codigo
    ) {
        boolean codigoExistente =
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoId,
                    codigo
                );

        if (codigoExistente) {
            throw new ConflitoDadosException(
                "Ja existe uma vaga com este "
                    + "codigo na organizacao"
            );
        }
    }

    private void validarCodigoDisponivelNaAtualizacao(
        UUID organizacaoId,
        String codigo,
        UUID vagaId
    ) {
        boolean codigoExistente =
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
                    organizacaoId,
                    codigo,
                    vagaId
                );

        if (codigoExistente) {
            throw new ConflitoDadosException(
                "Ja existe uma vaga com este "
                    + "codigo na organizacao"
            );
        }
    }

    private void validarDados(
        TipoVaga tipo,
        BigDecimal comprimentoMaximo,
        BigDecimal bocaMaxima,
        BigDecimal caladoMaximo,
        BigDecimal alturaMaxima,
        BigDecimal pesoMaximo,
        Boolean possuiAgua,
        Boolean possuiEnergia
    ) {
        if (tipo == null) {
            throw new DadosInvalidosException(
                "TIPO_VAGA_OBRIGATORIO",
                "Tipo de vaga obrigatorio"
            );
        }

        validarMedidaObrigatoria(
            comprimentoMaximo,
            "COMPRIMENTO_MAXIMO_INVALIDO",
            "Comprimento maximo deve ser "
                + "maior que zero"
        );

        validarMedidaObrigatoria(
            bocaMaxima,
            "BOCA_MAXIMA_INVALIDA",
            "Boca maxima deve ser maior "
                + "que zero"
        );

        validarMedidaOpcional(
            caladoMaximo,
            "CALADO_MAXIMO_INVALIDO",
            "Calado maximo deve ser maior "
                + "que zero"
        );

        validarMedidaOpcional(
            alturaMaxima,
            "ALTURA_MAXIMA_INVALIDA",
            "Altura maxima deve ser maior "
                + "que zero"
        );

        validarMedidaOpcional(
            pesoMaximo,
            "PESO_MAXIMO_INVALIDO",
            "Peso maximo deve ser maior "
                + "que zero"
        );

        if (possuiAgua == null) {
            throw new DadosInvalidosException(
                "POSSUI_AGUA_OBRIGATORIO",
                "Informacao sobre fornecimento "
                    + "de agua obrigatoria"
            );
        }

        if (possuiEnergia == null) {
            throw new DadosInvalidosException(
                "POSSUI_ENERGIA_OBRIGATORIO",
                "Informacao sobre fornecimento "
                    + "de energia obrigatoria"
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
                || valor.compareTo(
                    BigDecimal.ZERO
                ) <= 0
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
                && valor.compareTo(
                    BigDecimal.ZERO
                ) <= 0
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

    private String normalizarCodigo(
        String codigo
    ) {
        if (
            codigo == null
                || codigo.isBlank()
        ) {
            throw new DadosInvalidosException(
                "CODIGO_VAGA_OBRIGATORIO",
                "Codigo da vaga obrigatorio"
            );
        }

        return codigo
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    private String normalizarTermoBusca(
        String termo,
        String codigoErro,
        String mensagemErro
    ) {
        if (
            termo == null
                || termo.isBlank()
        ) {
            throw new DadosInvalidosException(
                codigoErro,
                mensagemErro
            );
        }

        return termo
            .trim()
            .replaceAll("\\s+", " ");
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