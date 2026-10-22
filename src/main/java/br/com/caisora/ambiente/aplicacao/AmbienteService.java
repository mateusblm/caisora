package br.com.caisora.ambiente.aplicacao;

import br.com.caisora.ambiente.api.PrevisaoResponse;
import br.com.caisora.ambiente.infraestrutura.openmeteo.OpenMeteoClient;
import br.com.caisora.organizacao.aplicacao.OrganizacaoService;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AmbienteService {

    private final OrganizacaoService organizacoes;
    private final OpenMeteoClient client;
    private final boolean habilitado;

    public AmbienteService(
            OrganizacaoService organizacoes,
            OpenMeteoClient client,
            @Value("${caisora.open-meteo.habilitado:false}") boolean habilitado
    ) {
        this.organizacoes = organizacoes;
        this.client = client;
        this.habilitado = habilitado;
    }

    public PrevisaoResponse consultar(UUID organizacaoId, boolean mar, LocalDate inicio, LocalDate fim) {
        var hoje = LocalDate.now(ZoneOffset.UTC);
        if (inicio == null || fim == null || fim.isBefore(inicio)
                || inicio.isBefore(hoje) || fim.isAfter(hoje.plusDays(15))) {
            throw new DadosInvalidosException(
                    "PERIODO_INVALIDO", "Selecione datas entre hoje e os próximos 15 dias, em UTC");
        }
        var coordenadas = organizacoes.coordenadas(organizacaoId);
        if (coordenadas.latitude() == null || coordenadas.longitude() == null) {
            throw new DadosInvalidosException("COORDENADAS_AUSENTES", "Configure as coordenadas da marina");
        }
        if (!habilitado) {
            throw new DadosInvalidosException("INTEGRACAO_DESABILITADA",
                    "Integração desabilitada. A plataforma deve revisar os termos e configurar o acesso Open-Meteo");
        }
        return client.consultar(mar, coordenadas.latitude(), coordenadas.longitude(), inicio, fim);
    }
}
