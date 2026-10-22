package br.com.caisora.referencia.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.caisora.referencia.aplicacao.*;
import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.compartilhado.excecao.TratadorGlobalException;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReferenciasControllerTest {
    private MockMvc mvc;private ReferenciasService service;
    @BeforeEach void configurar(){service=mock(ReferenciasService.class);mvc=MockMvcBuilders.standaloneSetup(new ReferenciasController(service,mock(LeitorTokenJwt.class))).setControllerAdvice(new TratadorGlobalException()).build();}
    @Test void parametrosObrigatoriosEFormatosInvalidosRetornam400() throws Exception {
        mvc.perform(get("/api/v1/referencias/municipios")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/referencias/feriados")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/referencias/feriados").param("ano","2026.5")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/referencias/mares").param("inicio","invalido")).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void indisponibilidadeExternaNaoExibeConteudoBruto() throws Exception {
        when(service.paises(null)).thenThrow(new ReferenciaIndisponivelException());
        mvc.perform(get("/api/v1/referencias/paises")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("PROVEDOR_INDISPONIVEL"));
    }
}
