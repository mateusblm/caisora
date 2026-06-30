package br.com.caisora.movimentacao.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.movimentacao.api.PosicaoEmbarcacaoResponse;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacao;
import br.com.caisora.movimentacao.dominio.PosicaoEmbarcacaoRepository;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosicaoEmbarcacaoService {

    private final PosicaoEmbarcacaoRepository
        posicaoRepository;

    private final EmbarcacaoRepository
        embarcacaoRepository;

    private final OcupacaoRepository
        ocupacaoRepository;

    private final PosicaoEmbarcacaoMapper
        posicaoMapper;

    private final LeitorTokenJwt leitorTokenJwt;

    public PosicaoEmbarcacaoService(
        PosicaoEmbarcacaoRepository
            posicaoRepository,
        EmbarcacaoRepository
            embarcacaoRepository,
        OcupacaoRepository
            ocupacaoRepository,
        PosicaoEmbarcacaoMapper
            posicaoMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.posicaoRepository =
            posicaoRepository;
        this.embarcacaoRepository =
            embarcacaoRepository;
        this.ocupacaoRepository =
            ocupacaoRepository;
        this.posicaoMapper = posicaoMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public PosicaoEmbarcacaoResponse
    buscarPorEmbarcacao(
        UUID embarcacaoId
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        Embarcacao embarcacao =
            buscarEmbarcacao(
                embarcacaoId,
                organizacaoId
            );

        PosicaoEmbarcacao posicao =
            obterOuCriarPosicao(
                embarcacao,
                organizacaoId
            );

        return posicaoMapper.paraResponse(
            posicao
        );
    }

    @Transactional(readOnly = true)
    public Page<PosicaoEmbarcacaoResponse>
    listar(
        TipoPosicaoEmbarcacao tipo,
        Pageable paginacao
    ) {
        UUID organizacaoId =
            obterOrganizacaoAutenticada();

        if (tipo == null) {
            return posicaoRepository
                .findAllByOrganizacaoId(
                    organizacaoId,
                    paginacao
                )
                .map(
                    posicaoMapper::paraResponse
                );
        }

        return posicaoRepository
            .findAllByOrganizacaoIdAndTipo(
                organizacaoId,
                tipo,
                paginacao
            )
            .map(posicaoMapper::paraResponse);
    }

    PosicaoEmbarcacao obterOuCriarPosicao(
        Embarcacao embarcacao,
        UUID organizacaoId
    ) {
        return posicaoRepository
            .findByOrganizacaoIdAndEmbarcacaoId(
                organizacaoId,
                embarcacao.getId()
            )
            .orElseGet(
                () -> criarPosicaoInicial(
                    embarcacao,
                    organizacaoId
                )
            );
    }

    private PosicaoEmbarcacao
    criarPosicaoInicial(
        Embarcacao embarcacao,
        UUID organizacaoId
    ) {
        Ocupacao ocupacaoAtiva =
            ocupacaoRepository
                .findByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                    organizacaoId,
                    embarcacao.getId(),
                    StatusOcupacao.ATIVA
                )
                .orElse(null);

        PosicaoEmbarcacao posicao;

        if (ocupacaoAtiva != null) {
            posicao =
                PosicaoEmbarcacao.criarEmVaga(
                    embarcacao.getOrganizacao(),
                    embarcacao,
                    ocupacaoAtiva.getVaga()
                );
        } else {
            posicao =
                PosicaoEmbarcacao
                    .criarDesconhecida(
                        embarcacao
                            .getOrganizacao(),
                        embarcacao
                    );
        }

        return posicaoRepository.save(posicao);
    }

    private Embarcacao buscarEmbarcacao(
        UUID embarcacaoId,
        UUID organizacaoId
    ) {
        if (embarcacaoId == null) {
            throw new DadosInvalidosException(
                "EMBARCACAO_OBRIGATORIA",
                "Embarcacao obrigatoria"
            );
        }

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

    private UUID obterOrganizacaoAutenticada() {
        return leitorTokenJwt
            .obterUsuarioAutenticado()
            .organizacaoId();
    }
}
