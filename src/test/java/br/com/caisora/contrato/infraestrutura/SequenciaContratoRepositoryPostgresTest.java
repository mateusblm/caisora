package br.com.caisora.contrato.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SequenciaContratoRepositoryPostgresTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void deveEnviarDataCompativelComTimestampComFuso() {
        when(
            jdbcTemplate.queryForObject(
                anyString(),
                anyMap(),
                eq(Long.class)
            )
        ).thenReturn(1L);

        SequenciaContratoRepositoryPostgres repository =
            new SequenciaContratoRepositoryPostgres(
                jdbcTemplate
            );

        long numero = repository.proximoNumero(
            UUID.randomUUID(),
            2026
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> parametros =
            ArgumentCaptor.forClass(Map.class);

        verify(jdbcTemplate).queryForObject(
            anyString(),
            parametros.capture(),
            eq(Long.class)
        );

        assertThat(numero).isEqualTo(1L);
        assertThat(
            parametros.getValue().get("atualizadoEm")
        ).isInstanceOf(OffsetDateTime.class);
    }
}
