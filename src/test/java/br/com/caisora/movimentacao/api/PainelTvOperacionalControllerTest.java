package br.com.caisora.movimentacao.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caisora.movimentacao.aplicacao.PainelTvOperacionalService;
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
class PainelTvOperacionalControllerTest {

    @Mock
    private PainelTvOperacionalService
        painelTvOperacionalService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        PainelTvOperacionalController controller =
            new PainelTvOperacionalController(
                painelTvOperacionalService
            );

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void deveBuscarPainelTv() throws Exception {
        PainelTvOperacionalResponse response =
            new PainelTvOperacionalResponse(
                Instant.parse(
                    "2026-06-30T15:00:00Z"
                ),
                "America/Sao_Paulo",
                Instant.parse(
                    "2026-06-30T03:00:00Z"
                ),
                Instant.parse(
                    "2026-07-01T02:59:59.999999999Z"
                ),
                15,
                new ResumoPainelTvResponse(
                    2,
                    1,
                    1,
                    0,
                    1,
                    2
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            );

        when(
            painelTvOperacionalService.buscar()
        ).thenReturn(response);

        mockMvc.perform(
                get(
                    "/api/v1/movimentacoes/"
                    + "painel-tv"
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.fusoHorario")
                    .value(
                        "America/Sao_Paulo"
                    )
            )
            .andExpect(
                jsonPath(
                    "$.atualizarAposSegundos"
                ).value(15)
            )
            .andExpect(
                jsonPath(
                    "$.resumo.descidasParaAgua"
                ).value(2)
            )
            .andExpect(
                jsonPath(
                    "$.resumo.retiradasDaAgua"
                ).value(1)
            )
            .andExpect(
                jsonPath(
                    "$.resumo.emExecucao"
                ).value(1)
            );
    }
}
