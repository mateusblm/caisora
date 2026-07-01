package br.com.caisora.movimentacao.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.movimentacao.aplicacao.PainelOperacionalService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PainelOperacionalControllerTest {

    @Mock
    private PainelOperacionalService painelOperacionalService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        PainelOperacionalController controller =
            new PainelOperacionalController(
                painelOperacionalService
            );

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void deveBuscarPainelOperacional() throws Exception {
        Instant agora =
            Instant.parse(
                "2026-06-30T15:00:00Z"
            );

        IndicadoresOperacionaisResponse indicadores =
            new IndicadoresOperacionaisResponse(
                2,
                1,
                3,
                2,
                1,
                8
            );

        PainelOperacionalResponse response =
            new PainelOperacionalResponse(
                agora,
                "America/Sao_Paulo",
                Instant.parse(
                    "2026-06-30T03:00:00Z"
                ),
                Instant.parse(
                    "2026-07-01T02:59:59.999999999Z"
                ),
                indicadores,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            );

        when(
            painelOperacionalService.buscar()
        ).thenReturn(response);

        mockMvc.perform(
                get(
                    "/api/v1/movimentacoes/"
                    + "painel-operacional"
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.fusoHorario")
                    .value("America/Sao_Paulo")
            )
            .andExpect(
                jsonPath(
                    "$.indicadores.emExecucao"
                ).value(2)
            )
            .andExpect(
                jsonPath(
                    "$.indicadores.atrasadas"
                ).value(1)
            )
            .andExpect(
                jsonPath(
                    "$.indicadores.concluidasHoje"
                ).value(8)
            );
    }
}
