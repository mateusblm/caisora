package br.com.caisora.vaga.api;

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
import br.com.caisora.vaga.aplicacao.VagaService;
import br.com.caisora.vaga.dominio.TipoVaga;
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
class VagaControllerTest {

    @Mock
    private VagaService vagaService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator =
            new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        VagaController controller =
            new VagaController(vagaService);

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
    void deveCriarVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();
        UUID organizacaoId = UUID.randomUUID();

        VagaResponse response = criarResponse(
            vagaId,
            organizacaoId,
            true
        );

        when(
            vagaService.criar(
                any(CriarVagaRequest.class)
            )
        ).thenReturn(response);

        mockMvc.perform(
            post("/api/v1/vagas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonCriacao())
        )
            .andExpect(status().isCreated())
            .andExpect(
                header().string(
                    "Location",
                    "/api/v1/vagas/" + vagaId
                )
            )
            .andExpect(
                jsonPath("$.id")
                    .value(vagaId.toString())
            )
            .andExpect(
                jsonPath("$.codigo")
                    .value("A-01")
            )
            .andExpect(
                jsonPath("$.tipo")
                    .value("MOLHADA")
            )
            .andExpect(
                jsonPath("$.possuiAgua")
                    .value(true)
            )
            .andExpect(
                jsonPath("$.possuiEnergia")
                    .value(true)
            )
            .andExpect(
                jsonPath("$.ativa")
                    .value(true)
            )
            .andExpect(
                jsonPath("$.organizacaoId")
                    .value(organizacaoId.toString())
            );
    }

    @Test
    void deveRetornarBadRequestAoCriarVagaInvalida()
        throws Exception {

        mockMvc.perform(
            post("/api/v1/vagas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigo": "",
                      "tipo": null,
                      "comprimentoMaximoMetros": 0,
                      "bocaMaximaMetros": 0,
                      "possuiAgua": null,
                      "possuiEnergia": null
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
    void deveRetornarConflitoParaCodigoDuplicado()
        throws Exception {

        when(
            vagaService.criar(
                any(CriarVagaRequest.class)
            )
        ).thenThrow(
            new ConflitoDadosException(
                "Ja existe uma vaga com este "
                    + "codigo na organizacao"
            )
        );

        mockMvc.perform(
            post("/api/v1/vagas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonCriacao())
        )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "Ja existe uma vaga com este "
                            + "codigo na organizacao"
                    )
            );
    }

    @Test
    void deveListarVagas() throws Exception {
        UUID organizacaoId = UUID.randomUUID();

        VagaResponse response = criarResponse(
            UUID.randomUUID(),
            organizacaoId,
            true
        );

        when(
            vagaService.listar(
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
            get("/api/v1/vagas")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].id")
                    .value(response.id().toString())
            )
            .andExpect(
                jsonPath("$.content[0].codigo")
                    .value("A-01")
            )
            .andExpect(
                jsonPath(
                    "$.content[0].organizacaoId"
                ).value(organizacaoId.toString())
            )
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1)
            );
    }

    @Test
    void deveBuscarVagasPorCodigo() throws Exception {
        VagaResponse response = criarResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            true
        );

        when(
            vagaService.buscarPorCodigo(
                eq("A-01"),
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
            get("/api/v1/vagas")
                .param("codigo", "A-01")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].codigo")
                    .value("A-01")
            )
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1)
            );

        verify(vagaService).buscarPorCodigo(
            eq("A-01"),
            any(Pageable.class)
        );
    }

    @Test
    void deveBuscarVagasPorSetor() throws Exception {
        VagaResponse response = criarResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            true
        );

        when(
            vagaService.buscarPorSetor(
                eq("Pier A"),
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
            get("/api/v1/vagas")
                .param("setor", "Pier A")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].setor")
                    .value("Pier A")
            );

        verify(vagaService).buscarPorSetor(
            eq("Pier A"),
            any(Pageable.class)
        );
    }

    @Test
    void deveListarVagasPorTipo() throws Exception {
        VagaResponse response = criarResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            true
        );

        when(
            vagaService.listarPorTipo(
                eq(TipoVaga.MOLHADA),
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
            get("/api/v1/vagas")
                .param("tipo", "MOLHADA")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].tipo")
                    .value("MOLHADA")
            );

        verify(vagaService).listarPorTipo(
            eq(TipoVaga.MOLHADA),
            any(Pageable.class)
        );
    }

    @Test
    void deveListarVagasPorStatus() throws Exception {
        VagaResponse response = criarResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            true
        );

        when(
            vagaService.listarPorStatus(
                eq(true),
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
            get("/api/v1/vagas")
                .param("ativa", "true")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].ativa")
                    .value(true)
            );

        verify(vagaService).listarPorStatus(
            eq(true),
            any(Pageable.class)
        );
    }

    @Test
    void deveBuscarVagaPorId() throws Exception {
        UUID vagaId = UUID.randomUUID();

        VagaResponse response = criarResponse(
            vagaId,
            UUID.randomUUID(),
            true
        );

        when(vagaService.buscarPorId(vagaId))
            .thenReturn(response);

        mockMvc.perform(
            get("/api/v1/vagas/{id}", vagaId)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(vagaId.toString())
            )
            .andExpect(
                jsonPath("$.codigo")
                    .value("A-01")
            );
    }

    @Test
    void deveRetornarNotFoundAoBuscarVagaInexistente()
        throws Exception {

        UUID vagaId = UUID.randomUUID();

        when(vagaService.buscarPorId(vagaId))
            .thenThrow(
                new RecursoNaoEncontradoException(
                    "Vaga nao encontrada"
                )
            );

        mockMvc.perform(
            get("/api/v1/vagas/{id}", vagaId)
        )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.mensagem")
                    .value("Vaga nao encontrada")
            );
    }

    @Test
    void deveAtualizarVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();

        VagaResponse response = new VagaResponse(
            vagaId,
            "B-02",
            TipoVaga.SECA,
            "Pátio B",
            "Área coberta",
            new BigDecimal("14.00"),
            new BigDecimal("4.50"),
            new BigDecimal("1.80"),
            new BigDecimal("6.00"),
            new BigDecimal("12000.00"),
            false,
            true,
            "Vaga atualizada",
            true,
            UUID.randomUUID(),
            Instant.parse("2026-06-29T20:00:00Z"),
            Instant.parse("2026-06-29T21:00:00Z")
        );

        when(
            vagaService.atualizar(
                eq(vagaId),
                any(AtualizarVagaRequest.class)
            )
        ).thenReturn(response);

        mockMvc.perform(
            put("/api/v1/vagas/{id}", vagaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonAtualizacao())
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(vagaId.toString())
            )
            .andExpect(
                jsonPath("$.codigo")
                    .value("B-02")
            )
            .andExpect(
                jsonPath("$.tipo")
                    .value("SECA")
            )
            .andExpect(
                jsonPath("$.possuiAgua")
                    .value(false)
            );
    }

    @Test
    void deveAlterarStatusDaVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();

        VagaResponse response = criarResponse(
            vagaId,
            UUID.randomUUID(),
            false
        );

        when(
            vagaService.alterarStatus(
                eq(vagaId),
                any(AlterarStatusVagaRequest.class)
            )
        ).thenReturn(response);

        mockMvc.perform(
            patch("/api/v1/vagas/{id}/status", vagaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "ativa": false
                    }
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(vagaId.toString())
            )
            .andExpect(
                jsonPath("$.ativa")
                    .value(false)
            );
    }

    @Test
    void deveRetornarBadRequestAoAlterarStatusNulo()
        throws Exception {

        mockMvc.perform(
            patch(
                "/api/v1/vagas/{id}/status",
                UUID.randomUUID()
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "ativa": null
                    }
                    """)
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.codigo")
                    .value("ERRO_VALIDACAO")
            );
    }

    private VagaResponse criarResponse(
        UUID vagaId,
        UUID organizacaoId,
        boolean ativa
    ) {
        return new VagaResponse(
            vagaId,
            "A-01",
            TipoVaga.MOLHADA,
            "Pier A",
            "Corredor principal",
            new BigDecimal("12.50"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("5.00"),
            new BigDecimal("9000.00"),
            true,
            true,
            "Próxima à recepção",
            ativa,
            organizacaoId,
            Instant.parse("2026-06-29T20:00:00Z"),
            Instant.parse("2026-06-29T20:00:00Z")
        );
    }

    private String jsonCriacao() {
        return """
            {
              "codigo": "A-01",
              "tipo": "MOLHADA",
              "setor": "Pier A",
              "localizacao": "Corredor principal",
              "comprimentoMaximoMetros": 12.50,
              "bocaMaximaMetros": 4.00,
              "caladoMaximoMetros": 1.50,
              "alturaMaximaMetros": 5.00,
              "pesoMaximoKg": 9000.00,
              "possuiAgua": true,
              "possuiEnergia": true,
              "observacoes": "Próxima à recepção"
            }
            """;
    }

    private String jsonAtualizacao() {
        return """
            {
              "codigo": "B-02",
              "tipo": "SECA",
              "setor": "Pátio B",
              "localizacao": "Área coberta",
              "comprimentoMaximoMetros": 14.00,
              "bocaMaximaMetros": 4.50,
              "caladoMaximoMetros": 1.80,
              "alturaMaximaMetros": 6.00,
              "pesoMaximoKg": 12000.00,
              "possuiAgua": false,
              "possuiEnergia": true,
              "observacoes": "Vaga atualizada"
            }
            """;
    }
}
