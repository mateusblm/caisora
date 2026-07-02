package br.com.caisora.compartilhado.configuracao;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class ConfiguracaoCors {

    private static final List<String> METODOS_PERMITIDOS =
        List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        );

    @Bean
    FilterRegistrationBean<CorsFilter> filtroCors(
        @Value(
            "${caisora.cors.origens-permitidas:"
                + "http://localhost:4200}"
        )
        String origensPermitidas
    ) {
        CorsConfiguration configuracao =
            new CorsConfiguration();

        configuracao.setAllowedOriginPatterns(
            separarOrigens(origensPermitidas)
        );
        configuracao.setAllowedMethods(
            METODOS_PERMITIDOS
        );
        configuracao.setAllowedHeaders(
            List.of("*")
        );
        configuracao.setExposedHeaders(
            List.of("Location")
        );
        configuracao.setAllowCredentials(false);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte =
            new UrlBasedCorsConfigurationSource();

        fonte.registerCorsConfiguration(
            "/api/**",
            configuracao
        );

        FilterRegistrationBean<CorsFilter> registro =
            new FilterRegistrationBean<>(
                new CorsFilter(fonte)
            );

        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }

    private static List<String> separarOrigens(
        String origensPermitidas
    ) {
        return Arrays.stream(
                origensPermitidas.split(",")
            )
            .map(String::trim)
            .filter(origem -> !origem.isBlank())
            .distinct()
            .toList();
    }
}
