package br.com.caisora.ocupacao.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.ocupacao.aplicacao.OcupacaoService;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.vaga.dominio.TipoVaga;
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
class OcupacaoControllerTest {

    @Mock
    private OcupacaoService ocupacaoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator =
            new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        OcupacaoController controller =
            new OcupacaoController(
                ocupacaoService
            );

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
    void deveCriarOcupacao() throws Exception {
        UUID ocupacaoId = UUID.randomUUID();
        UUID embarcacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        OcupacaoResponse response =
            criarResponse(
                ocupacaoId,
                embarcacaoId,
                vagaId,
                organizacaoId,
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService.criar(
                any(CriarOcupacaoRequest.class)
            )
        ).thenReturn(response);

        mockMvc.perform(
            post("/api/v1/ocupacoes")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    jsonCriacao(
                        embarcacaoId,
                        vagaId
                    )
                )
        )
            .andExpect(status().isCreated())
            .andExpect(
                header().string(
                    "Location",
                    "/api/v1/ocupacoes/"
                        + ocupacaoId
                )
            )
            .andExpect(
                jsonPath("$.id")
                    .value(
                        ocupacaoId.toString()
                    )
            )
            .andExpect(
                jsonPath("$.embarcacaoId")
                    .value(
                        embarcacaoId.toString()
                    )
            )
            .andExpect(
                jsonPath("$.embarcacaoNome")
                    .value("Aurora")
            )
            .andExpect(
                jsonPath("$.vagaId")
                    .value(vagaId.toString())
            )
            .andExpect(
                jsonPath("$.vagaCodigo")
                    .value("A-01")
            )
            .andExpect(
                jsonPath("$.status")
                    .value("ATIVA")
            )
            .andExpect(
                jsonPath("$.organizacaoId")
                    .value(
                        organizacaoId.toString()
                    )
            );
    }

    @Test
    void deveRetornarBadRequestAoCriarInvalida()
        throws Exception {

        mockMvc.perform(
            post("/api/v1/ocupacoes")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content("""
                    {
                      "embarcacaoId": null,
                      "vagaId": null,
                      "inicioEm": null,
                      "fimPrevistoEm": null,
                      "observacoes": null
                    }
                    """)
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.codigo")
                    .value("ERRO_VALIDACAO")
            )
            .andExpect(
                jsonPath("$.errosCampos")
                    .isArray()
            );
    }

    @Test
    void deveRetornarConflitoAoCriarEmVagaOcupada()
        throws Exception {

        when(
            ocupacaoService.criar(
                any(CriarOcupacaoRequest.class)
            )
        ).thenThrow(
            new ConflitoDadosException(
                "A vaga ja possui uma "
                    + "ocupacao ativa"
            )
        );

        mockMvc.perform(
            post("/api/v1/ocupacoes")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    jsonCriacao(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                    )
                )
        )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.codigo")
                    .value("CONFLITO_DADOS")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "A vaga ja possui uma "
                            + "ocupacao ativa"
                    )
            );
    }

    @Test
    void deveListarOcupacoes() throws Exception {
        OcupacaoResponse response =
            criarResponsePadrao(
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService.listar(
                any(Pageable.class)
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 10),
                1
            )
        );

        mockMvc.perform(
            get("/api/v1/ocupacoes")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].id")
                    .value(
                        response.id().toString()
                    )
            )
            .andExpect(
                jsonPath(
                    "$.content[0].embarcacaoNome"
                ).value("Aurora")
            )
            .andExpect(
                jsonPath(
                    "$.content[0].vagaCodigo"
                ).value("A-01")
            )
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1)
            );
    }

    @Test
    void deveListarPorEmbarcacao() throws Exception {
        UUID embarcacaoId = UUID.randomUUID();

        OcupacaoResponse response =
            criarResponse(
                UUID.randomUUID(),
                embarcacaoId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService
                .listarPorEmbarcacao(
                    eq(embarcacaoId),
                    any(Pageable.class)
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
            )
        );

        mockMvc.perform(
            get("/api/v1/ocupacoes")
                .param(
                    "embarcacaoId",
                    embarcacaoId.toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$.content[0].embarcacaoId"
                ).value(
                    embarcacaoId.toString()
                )
            );

        verify(ocupacaoService)
            .listarPorEmbarcacao(
                eq(embarcacaoId),
                any(Pageable.class)
            );
    }

    @Test
    void deveListarPorVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();

        OcupacaoResponse response =
            criarResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                vagaId,
                UUID.randomUUID(),
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService.listarPorVaga(
                eq(vagaId),
                any(Pageable.class)
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
            )
        );

        mockMvc.perform(
            get("/api/v1/ocupacoes")
                .param(
                    "vagaId",
                    vagaId.toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].vagaId")
                    .value(vagaId.toString())
            );

        verify(ocupacaoService)
            .listarPorVaga(
                eq(vagaId),
                any(Pageable.class)
            );
    }

    @Test
    void deveListarPorStatus() throws Exception {
        OcupacaoResponse response =
            criarResponsePadrao(
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService.listarPorStatus(
                eq(StatusOcupacao.ATIVA),
                any(Pageable.class)
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
            )
        );

        mockMvc.perform(
            get("/api/v1/ocupacoes")
                .param("status", "ATIVA")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].status")
                    .value("ATIVA")
            );

        verify(ocupacaoService)
            .listarPorStatus(
                eq(StatusOcupacao.ATIVA),
                any(Pageable.class)
            );
    }

   @Test
    void devePriorizarFiltroDeEmbarcacao()
        throws Exception {

        UUID embarcacaoId = UUID.randomUUID();
        UUID vagaId = UUID.randomUUID();

        when(
            ocupacaoService
                .listarPorEmbarcacao(
                    eq(embarcacaoId),
                    any(Pageable.class)
                )
        ).thenReturn(
            new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
            )
        );

        mockMvc.perform(
            get("/api/v1/ocupacoes")
                .param(
                    "embarcacaoId",
                    embarcacaoId.toString()
                )
                .param(
                    "vagaId",
                    vagaId.toString()
                )
                .param("status", "ATIVA")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isEmpty()
            )
            .andExpect(
                jsonPath("$.totalElements")
                    .value(0)
            );

        verify(ocupacaoService)
            .listarPorEmbarcacao(
                eq(embarcacaoId),
                any(Pageable.class)
            );
    }

    @Test
    void deveBuscarOcupacaoPorId()
        throws Exception {

        UUID ocupacaoId = UUID.randomUUID();

        OcupacaoResponse response =
            criarResponse(
                ocupacaoId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                StatusOcupacao.ATIVA,
                null
            );

        when(
            ocupacaoService.buscarPorId(
                ocupacaoId
            )
        ).thenReturn(response);

        mockMvc.perform(
            get(
                "/api/v1/ocupacoes/{id}",
                ocupacaoId
            )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(
                        ocupacaoId.toString()
                    )
            );
    }

    @Test
    void deveRetornarNotFoundParaOcupacaoInexistente()
        throws Exception {

        UUID ocupacaoId = UUID.randomUUID();

        when(
            ocupacaoService.buscarPorId(
                ocupacaoId
            )
        ).thenThrow(
            new RecursoNaoEncontradoException(
                "Ocupacao nao encontrada"
            )
        );

        mockMvc.perform(
            get(
                "/api/v1/ocupacoes/{id}",
                ocupacaoId
            )
        )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.codigo")
                    .value(
                        "RECURSO_NAO_ENCONTRADO"
                    )
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "Ocupacao nao encontrada"
                    )
            );
    }

    @Test
    void deveAtualizarOcupacao()
        throws Exception {

        UUID ocupacaoId = UUID.randomUUID();
        Instant novoFim =
            Instant.parse(
                "2026-07-10T18:00:00Z"
            );

        OcupacaoResponse response =
            new OcupacaoResponse(
                ocupacaoId,
                UUID.randomUUID(),
                "Aurora",
                "V33",
                "Joao da Silva",
                UUID.randomUUID(),
                "A-01",
                TipoVaga.MOLHADA,
                "Pier A",
                "Corredor principal",
                StatusOcupacao.ATIVA,
                Instant.parse(
                    "2026-06-29T18:00:00Z"
                ),
                novoFim,
                null,
                "Nova previsão",
                UUID.randomUUID(),
                Instant.parse(
                    "2026-06-29T18:00:00Z"
                ),
                Instant.parse(
                    "2026-06-29T19:00:00Z"
                )
            );

        when(
            ocupacaoService.atualizar(
                eq(ocupacaoId),
                any(
                    AtualizarOcupacaoRequest.class
                )
            )
        ).thenReturn(response);

        mockMvc.perform(
            put(
                "/api/v1/ocupacoes/{id}",
                ocupacaoId
            )
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content("""
                    {
                      "fimPrevistoEm":
                        "2026-07-10T18:00:00Z",
                      "observacoes":
                        "Nova previsão"
                    }
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(
                        ocupacaoId.toString()
                    )
            )
            .andExpect(
                jsonPath("$.fimPrevistoEm")
                    .value(
                        "2026-07-10T18:00:00Z"
                    )
            )
            .andExpect(
                jsonPath("$.observacoes")
                    .value("Nova previsão")
            );
    }

    @Test
    void deveEncerrarOcupacao()
        throws Exception {

        UUID ocupacaoId = UUID.randomUUID();
        Instant encerramento =
            Instant.parse(
                "2026-06-29T22:30:00Z"
            );

        OcupacaoResponse response =
            criarResponse(
                ocupacaoId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                StatusOcupacao.ENCERRADA,
                encerramento
            );

        when(
            ocupacaoService.encerrar(
                eq(ocupacaoId),
                any(
                    EncerrarOcupacaoRequest.class
                )
            )
        ).thenReturn(response);

        mockMvc.perform(
            patch(
                "/api/v1/ocupacoes/{id}/encerramento",
                ocupacaoId
            )
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content("""
                    {
                      "encerradaEm":
                        "2026-06-29T22:30:00Z"
                    }
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.status")
                    .value("ENCERRADA")
            )
            .andExpect(
                jsonPath("$.encerradaEm")
                    .value(
                        "2026-06-29T22:30:00Z"
                    )
            );
    }

    @Test
    void deveRetornarBadRequestAoEncerrarSemData()
        throws Exception {

        mockMvc.perform(
            patch(
                "/api/v1/ocupacoes/{id}/encerramento",
                UUID.randomUUID()
            )
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content("""
                    {
                      "encerradaEm": null
                    }
                    """)
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.codigo")
                    .value("ERRO_VALIDACAO")
            );
    }

    private OcupacaoResponse
    criarResponsePadrao(
        StatusOcupacao status,
        Instant encerradaEm
    ) {
        return criarResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            status,
            encerradaEm
        );
    }

    private OcupacaoResponse criarResponse(
        UUID ocupacaoId,
        UUID embarcacaoId,
        UUID vagaId,
        UUID organizacaoId,
        StatusOcupacao status,
        Instant encerradaEm
    ) {
        return new OcupacaoResponse(
            ocupacaoId,
            embarcacaoId,
            "Aurora",
            "V33",
            "Joao da Silva",
            vagaId,
            "A-01",
            TipoVaga.MOLHADA,
            "Pier A",
            "Corredor principal",
            status,
            Instant.parse(
                "2026-06-29T18:00:00Z"
            ),
            Instant.parse(
                "2026-07-10T18:00:00Z"
            ),
            encerradaEm,
            "Ocupação mensal",
            organizacaoId,
            Instant.parse(
                "2026-06-29T18:00:00Z"
            ),
            Instant.parse(
                "2026-06-29T18:00:00Z"
            )
        );
    }

    private String jsonCriacao(
        UUID embarcacaoId,
        UUID vagaId
    ) {
        return """
            {
              "embarcacaoId": "%s",
              "vagaId": "%s",
              "inicioEm":
                "2026-06-29T18:00:00Z",
              "fimPrevistoEm":
                "2026-07-10T18:00:00Z",
              "observacoes":
                "Ocupação mensal"
            }
            """.formatted(
                embarcacaoId,
                vagaId
            );
    }
}
