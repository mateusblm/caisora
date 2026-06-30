package br.com.caisora.movimentacao.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import br.com.caisora.movimentacao.aplicacao.MovimentacaoService;
import br.com.caisora.movimentacao.dominio.PrioridadeMovimentacao;
import br.com.caisora.movimentacao.dominio.StatusMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoEventoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoMovimentacao;
import br.com.caisora.movimentacao.dominio.TipoPosicaoEmbarcacao;

@ExtendWith(MockitoExtension.class)
class MovimentacaoControllerTest {

    @Mock
    private MovimentacaoService service;

    private MockMvc mockMvc;

    private UUID movimentacaoId;
    private UUID embarcacaoId;
    private UUID organizacaoId;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MovimentacaoController(service))
                .setControllerAdvice(new TratadorGlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();

        movimentacaoId = UUID.randomUUID();
        embarcacaoId = UUID.randomUUID();
        organizacaoId = UUID.randomUUID();
    }

    @Test
    void deveCriarMovimentacao() throws Exception {
        MovimentacaoResponse response = criarResponse(StatusMovimentacao.AGENDADA);
        when(service.criar(any(CriarMovimentacaoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "embarcacaoId": "%s",
                                  "tipo": "LANCAMENTO",
                                  "prioridade": "ALTA",
                                  "tipoPosicaoDestino": "AGUA",
                                  "descricaoDestino": "Canal principal",
                                  "agendadaPara": "2027-01-10T14:00:00Z",
                                  "observacoes": "Cliente presente na marina"
                                }
                                """.formatted(embarcacaoId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/movimentacoes/" + movimentacaoId))
                .andExpect(jsonPath("$.id").value(movimentacaoId.toString()))
                .andExpect(jsonPath("$.tipo").value("LANCAMENTO"))
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));
    }

    @Test
    void deveRejeitarCriacaoSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).criar(any(CriarMovimentacaoRequest.class));
    }

    @Test
    void deveListarMovimentacoesFiltradasPorStatus() throws Exception {
        MovimentacaoResponse response = criarResponse(StatusMovimentacao.AGENDADA);
        when(service.listar(
                eq(StatusMovimentacao.AGENDADA),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/movimentacoes")
                        .param("status", "AGENDADA")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(movimentacaoId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("AGENDADA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveIniciarMovimentacao() throws Exception {
        MovimentacaoResponse response = criarResponse(StatusMovimentacao.EM_EXECUCAO);
        when(service.iniciar(eq(movimentacaoId), any(IniciarMovimentacaoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/movimentacoes/{id}/inicio", movimentacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "iniciadaEm": "2027-01-10T13:55:00Z",
                                  "observacao": "Operador iniciou a movimentação"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void deveConcluirMovimentacao() throws Exception {
        MovimentacaoResponse response = criarResponse(StatusMovimentacao.CONCLUIDA);
        when(service.concluir(eq(movimentacaoId), any(ConcluirMovimentacaoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/movimentacoes/{id}/conclusao", movimentacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "concluidaEm": "2027-01-10T14:20:00Z",
                                  "observacao": "Movimentação concluída"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void deveRejeitarCancelamentoSemMotivo() throws Exception {
        mockMvc.perform(patch("/api/v1/movimentacoes/{id}/cancelamento", movimentacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canceladaEm": "2027-01-10T13:30:00Z",
                                  "motivo": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .cancelar(eq(movimentacaoId), any(CancelarMovimentacaoRequest.class));
    }

    @Test
    void deveListarHistoricoDaMovimentacao() throws Exception {
        HistoricoMovimentacaoResponse historico = new HistoricoMovimentacaoResponse(
                UUID.randomUUID(),
                movimentacaoId,
                TipoEventoMovimentacao.CRIADA,
                null,
                StatusMovimentacao.AGENDADA,
                null,
                Instant.parse("2027-01-10T14:00:00Z"),
                UUID.randomUUID(),
                "Operador",
                "Movimentação criada",
                Map.of(),
                Map.of("status", "AGENDADA"),
                organizacaoId,
                Instant.parse("2027-01-09T12:00:00Z"));

        when(service.listarHistorico(eq(movimentacaoId), any()))
                .thenReturn(new PageImpl<>(List.of(historico), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/movimentacoes/{id}/historico", movimentacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipoEvento").value("CRIADA"))
                .andExpect(jsonPath("$.content[0].statusNovo").value("AGENDADA"));
    }

    private MovimentacaoResponse criarResponse(StatusMovimentacao status) {
        Instant agendadaPara = Instant.parse("2027-01-10T14:00:00Z");
        Instant iniciadaEm = status == StatusMovimentacao.EM_EXECUCAO
                || status == StatusMovimentacao.CONCLUIDA
                        ? Instant.parse("2027-01-10T13:55:00Z")
                        : null;
        Instant concluidaEm = status == StatusMovimentacao.CONCLUIDA
                ? Instant.parse("2027-01-10T14:20:00Z")
                : null;

        return new MovimentacaoResponse(
                movimentacaoId,
                embarcacaoId,
                "Aurora",
                "Phantom 300",
                "Cliente Teste",
                TipoMovimentacao.LANCAMENTO,
                status,
                PrioridadeMovimentacao.ALTA,
                TipoPosicaoEmbarcacao.VAGA,
                UUID.randomUUID(),
                "A-01",
                null,
                TipoPosicaoEmbarcacao.AGUA,
                null,
                null,
                "Canal principal",
                agendadaPara,
                iniciadaEm,
                concluidaEm,
                null,
                UUID.randomUUID(),
                "Solicitante",
                UUID.randomUUID(),
                "Operador",
                "Cliente presente",
                null,
                0L,
                organizacaoId,
                Instant.parse("2027-01-09T12:00:00Z"),
                Instant.parse("2027-01-09T12:00:00Z"));
    }
}
