package br.com.caisora.embarcacao.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.embarcacao.aplicacao.EmbarcacaoService;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class EmbarcacaoControllerTest {

    @Mock
    private EmbarcacaoService embarcacaoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        EmbarcacaoController controller =
                new EmbarcacaoController(embarcacaoService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new TratadorGlobalException()
                )
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void deveCriarEmbarcacao() throws Exception {
        UUID embarcacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        EmbarcacaoResponse response = criarResponse(
                embarcacaoId,
                proprietarioId,
                organizacaoId,
                true
        );

        when(embarcacaoService.criar(
                any(CriarEmbarcacaoRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/embarcacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCriacao(proprietarioId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/embarcacoes/" + embarcacaoId
                ))
                .andExpect(jsonPath("$.id")
                        .value(embarcacaoId.toString()))
                .andExpect(jsonPath("$.proprietarioId")
                        .value(proprietarioId.toString()))
                .andExpect(jsonPath("$.proprietarioNome")
                        .value("Joao da Silva"))
                .andExpect(jsonPath("$.nome")
                        .value("Aurora"))
                .andExpect(jsonPath("$.tipo")
                        .value("LANCHA"))
                .andExpect(jsonPath("$.tipoPropulsao")
                        .value("MOTOR"))
                .andExpect(jsonPath("$.ativa")
                        .value(true))
                .andExpect(jsonPath("$.organizacaoId")
                        .value(organizacaoId.toString()));
    }

    @Test
    void deveRetornarBadRequestAoCriarEmbarcacaoInvalida()
            throws Exception {

        mockMvc.perform(post("/api/v1/embarcacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "proprietarioId": null,
                              "tipo": null,
                              "comprimentoTotalMetros": 0,
                              "bocaMetros": 0,
                              "tipoPropulsao": null
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ERRO_VALIDACAO"))
                .andExpect(jsonPath("$.errosCampos")
                        .isArray());
    }

    @Test
    void deveRetornarBadRequestParaRegraDeNegocioInvalida()
            throws Exception {

        when(embarcacaoService.criar(
                any(CriarEmbarcacaoRequest.class)
        )).thenThrow(new DadosInvalidosException(
                "ANO_FABRICACAO_INVALIDO",
                "Ano de fabricacao invalido"
        ));

        UUID proprietarioId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/embarcacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCriacao(proprietarioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ANO_FABRICACAO_INVALIDO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Ano de fabricacao invalido"));
    }

    @Test
    void deveRetornarConflitoParaInscricaoDuplicada()
            throws Exception {

        when(embarcacaoService.criar(
                any(CriarEmbarcacaoRequest.class)
        )).thenThrow(new ConflitoDadosException(
                "Ja existe uma embarcacao com este "
                        + "numero de inscricao na organizacao"
        ));

        mockMvc.perform(post("/api/v1/embarcacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCriacao(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value(
                                "Ja existe uma embarcacao com este "
                                        + "numero de inscricao "
                                        + "na organizacao"
                        ));
    }

    @Test
    void deveListarEmbarcacoes() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        EmbarcacaoResponse response = criarResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                organizacaoId,
                true
        );

        when(embarcacaoService.listar(
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 10),
                1
        ));

        mockMvc.perform(get("/api/v1/embarcacoes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id")
                        .value(response.id().toString()))
                .andExpect(jsonPath("$.content[0].nome")
                        .value("Aurora"))
                .andExpect(jsonPath(
                        "$.content[0].organizacaoId"
                ).value(organizacaoId.toString()))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }

    @Test
    void deveBuscarEmbarcacoesPorNome() throws Exception {
        EmbarcacaoResponse response = criarResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                true
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(embarcacaoService.buscarPorNome(
                eq("Aurora"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/embarcacoes")
                        .param("nome", "Aurora")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome")
                        .value("Aurora"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }

    @Test
    void deveListarEmbarcacoesPorProprietario()
            throws Exception {

        UUID proprietarioId = UUID.randomUUID();

        EmbarcacaoResponse response = criarResponse(
                UUID.randomUUID(),
                proprietarioId,
                UUID.randomUUID(),
                true
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(embarcacaoService.listarPorProprietario(
                eq(proprietarioId),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/embarcacoes")
                        .param(
                                "proprietarioId",
                                proprietarioId.toString()
                        )
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[0].proprietarioId"
                ).value(proprietarioId.toString()));
    }

    @Test
    void deveListarEmbarcacoesPorTipo() throws Exception {
        EmbarcacaoResponse response = criarResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                true
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(embarcacaoService.listarPorTipo(
                eq(TipoEmbarcacao.LANCHA),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/embarcacoes")
                        .param("tipo", "LANCHA")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo")
                        .value("LANCHA"));
    }

    @Test
    void deveListarEmbarcacoesPorStatus()
            throws Exception {

        EmbarcacaoResponse response = criarResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        PageRequest paginacao = PageRequest.of(0, 20);

        when(embarcacaoService.listarPorStatus(
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response),
                paginacao,
                1
        ));

        mockMvc.perform(get("/api/v1/embarcacoes")
                        .param("ativa", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ativa")
                        .value(false));
    }

    @Test
    void deveBuscarEmbarcacaoPorId() throws Exception {
        UUID embarcacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        when(embarcacaoService.buscarPorId(embarcacaoId))
                .thenReturn(criarResponse(
                        embarcacaoId,
                        proprietarioId,
                        organizacaoId,
                        true
                ));

        mockMvc.perform(get(
                        "/api/v1/embarcacoes/{id}",
                        embarcacaoId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(embarcacaoId.toString()))
                .andExpect(jsonPath("$.proprietarioId")
                        .value(proprietarioId.toString()))
                .andExpect(jsonPath("$.organizacaoId")
                        .value(organizacaoId.toString()));
    }

    @Test
    void deveRetornarNotFoundParaEmbarcacaoInexistente()
            throws Exception {

        UUID embarcacaoId = UUID.randomUUID();

        when(embarcacaoService.buscarPorId(embarcacaoId))
                .thenThrow(
                        new RecursoNaoEncontradoException(
                                "Embarcacao nao encontrada"
                        )
                );

        mockMvc.perform(get(
                        "/api/v1/embarcacoes/{id}",
                        embarcacaoId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Embarcacao nao encontrada"));
    }

    @Test
    void deveAtualizarEmbarcacao() throws Exception {
        UUID embarcacaoId = UUID.randomUUID();
        UUID proprietarioId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        EmbarcacaoResponse response = new EmbarcacaoResponse(
                embarcacaoId,
                proprietarioId,
                "Joao da Silva",
                "Aurora Atualizada",
                TipoEmbarcacao.LANCHA,
                "Schaefer",
                "V33",
                2024,
                "PR-654321",
                "BR-NOVO123",
                "Paranagua",
                "BR",
                new BigDecimal("10.50"),
                new BigDecimal("3.40"),
                new BigDecimal("1.00"),
                new BigDecimal("1.75"),
                new BigDecimal("3.70"),
                new BigDecimal("5400.00"),
                12,
                TipoPropulsao.MOTOR,
                "Branca e azul",
                "Dados atualizados",
                true,
                organizacaoId,
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T21:00:00Z")
        );

        when(embarcacaoService.atualizar(
                eq(embarcacaoId),
                any(AtualizarEmbarcacaoRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/api/v1/embarcacoes/{id}",
                        embarcacaoId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonAtualizacao(proprietarioId)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome")
                        .value("Aurora Atualizada"))
                .andExpect(jsonPath("$.numeroInscricao")
                        .value("PR-654321"))
                .andExpect(jsonPath("$.comprimentoTotalMetros")
                        .value(10.50));
    }

    @Test
    void deveAlterarStatusEmbarcacao() throws Exception {
        UUID embarcacaoId = UUID.randomUUID();

        EmbarcacaoResponse response = criarResponse(
                embarcacaoId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                false
        );

        when(embarcacaoService.alterarStatus(
                eq(embarcacaoId),
                any(AlterarStatusEmbarcacaoRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/embarcacoes/{id}/status",
                        embarcacaoId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ativa": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa")
                        .value(false));
    }

    @Test
    void deveRetornarBadRequestAoAlterarStatusSemValor()
            throws Exception {

        UUID embarcacaoId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/embarcacoes/{id}/status",
                        embarcacaoId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ativa": null
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("ERRO_VALIDACAO"));
    }

    private String jsonCriacao(UUID proprietarioId) {
        return """
                {
                  "proprietarioId": "%s",
                  "nome": "Aurora",
                  "tipo": "LANCHA",
                  "fabricante": "Schaefer",
                  "modelo": "V33",
                  "anoFabricacao": 2023,
                  "numeroInscricao": "PR-123456",
                  "numeroCasco": "BR-SCH12345A323",
                  "portoInscricao": "Paranagua",
                  "codigoPaisBandeira": "BR",
                  "comprimentoTotalMetros": 10.33,
                  "bocaMetros": 3.35,
                  "caladoMetros": 0.95,
                  "pontalMetros": 1.70,
                  "alturaTotalMetros": 3.60,
                  "pesoKg": 5200.00,
                  "capacidadePessoas": 12,
                  "tipoPropulsao": "MOTOR",
                  "corPredominante": "Branca",
                  "observacoes": "Embarcacao principal"
                }
                """.formatted(proprietarioId);
    }

    private String jsonAtualizacao(UUID proprietarioId) {
        return """
                {
                  "proprietarioId": "%s",
                  "nome": "Aurora Atualizada",
                  "tipo": "LANCHA",
                  "fabricante": "Schaefer",
                  "modelo": "V33",
                  "anoFabricacao": 2024,
                  "numeroInscricao": "PR-654321",
                  "numeroCasco": "BR-NOVO123",
                  "portoInscricao": "Paranagua",
                  "codigoPaisBandeira": "BR",
                  "comprimentoTotalMetros": 10.50,
                  "bocaMetros": 3.40,
                  "caladoMetros": 1.00,
                  "pontalMetros": 1.75,
                  "alturaTotalMetros": 3.70,
                  "pesoKg": 5400.00,
                  "capacidadePessoas": 12,
                  "tipoPropulsao": "MOTOR",
                  "corPredominante": "Branca e azul",
                  "observacoes": "Dados atualizados"
                }
                """.formatted(proprietarioId);
    }

    private EmbarcacaoResponse criarResponse(
            UUID id,
            UUID proprietarioId,
            UUID organizacaoId,
            boolean ativa
    ) {
        return new EmbarcacaoResponse(
                id,
                proprietarioId,
                "Joao da Silva",
                "Aurora",
                TipoEmbarcacao.LANCHA,
                "Schaefer",
                "V33",
                2023,
                "PR-123456",
                "BR-SCH12345A323",
                "Paranagua",
                "BR",
                new BigDecimal("10.33"),
                new BigDecimal("3.35"),
                new BigDecimal("0.95"),
                new BigDecimal("1.70"),
                new BigDecimal("3.60"),
                new BigDecimal("5200.00"),
                12,
                TipoPropulsao.MOTOR,
                "Branca",
                "Embarcacao principal",
                ativa,
                organizacaoId,
                Instant.parse("2026-06-28T20:00:00Z"),
                Instant.parse("2026-06-28T20:00:00Z")
        );
    }
}
