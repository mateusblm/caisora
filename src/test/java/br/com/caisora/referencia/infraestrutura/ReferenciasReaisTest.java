package br.com.caisora.referencia.infraestrutura;

import static org.assertj.core.api.Assertions.*;
import br.com.caisora.referencia.infraestrutura.http.TransporteJsonExterno;
import br.com.caisora.referencia.infraestrutura.ibge.IbgeClient;
import br.com.caisora.referencia.infraestrutura.brasilapi.BrasilApiClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/** Opt-in; apenas consultas pontuais aos provedores públicos, sem WorldTides ou PostgreSQL. */
@EnabledIfEnvironmentVariable(named="CAISORA_AIE_REAL",matches="true")
class ReferenciasReaisTest {
    @Test void registraConsultaRealDosTresCatalogosHabilitados() throws Exception {
        var http = new TransporteJsonExterno(10000, 20000);
        var ibge = new IbgeClient(http,"https://servicodados.ibge.gov.br");
        var brasil = new BrasilApiClient(http,"https://brasilapi.com.br");
        var registros = new ArrayList<Map<String,Object>>();
        var paises = ibge.paises();
        var br = paises.stream().filter(p -> "BR".equals(p.codigoIso2())).findFirst().orElseThrow();
        assertThat(br.nome()).isEqualTo("Brasil");
        registros.add(Map.of("grupo","AIE03","endpoint","https://servicodados.ibge.gov.br/api/v1/paises/-",
                "consultadoEm",Instant.now().toString(),"quantidade",paises.size(),"amostra",br));
        var municipios = ibge.municipios("SC");
        var florianopolis = municipios.stream().filter(m -> Integer.valueOf(4205407).equals(m.codigoIbge())).findFirst().orElseThrow();
        assertThat(florianopolis.siglaUf()).isEqualTo("SC");
        registros.add(Map.of("grupo","AIE04","endpoint","https://servicodados.ibge.gov.br/api/v1/localidades/estados/SC/municipios",
                "consultadoEm",Instant.now().toString(),"quantidade",municipios.size(),"amostra",florianopolis));
        var feriados = brasil.feriados(2026,"SC");
        assertThat(feriados).anyMatch(f -> "national".equals(f.tipo()));
        assertThat(feriados).anyMatch(f -> "state".equals(f.tipo()) && "SC".equals(f.uf()));
        registros.add(Map.of("grupo","AIE05","endpoint","https://brasilapi.com.br/api/feriados/v1/2026?uf=SC",
                "consultadoEm",Instant.now().toString(),"quantidade",feriados.size(),"amostra",feriados));
        var pasta=Path.of("docs/evidencias/apf-aie");Files.createDirectories(pasta);
        Files.writeString(pasta.resolve("consultas-reais.json"),JsonMapper.builder().build().writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of("modo","Chamadas HTTPS reais pelo cliente Java, sem mocks",
                "consultas",registros,"worldTides","Não consultada: sem credenciais e autorização; permanece desabilitada")));
    }
}
