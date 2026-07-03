package br.com.caisora.contrato.infraestrutura;

import br.com.caisora.contrato.dominio.SequenciaContratoRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SequenciaContratoRepositoryPostgres
    implements SequenciaContratoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SequenciaContratoRepositoryPostgres(
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long proximoNumero(UUID organizacaoId, int ano) {
        String sql = """
            insert into sequencias_contrato (
                organizacao_id,
                ano,
                ultimo_numero,
                atualizado_em
            ) values (
                :organizacaoId,
                :ano,
                1,
                :atualizadoEm
            )
            on conflict (organizacao_id, ano)
            do update set
                ultimo_numero = sequencias_contrato.ultimo_numero + 1,
                atualizado_em = excluded.atualizado_em
            returning ultimo_numero
            """;

        Long numero = jdbcTemplate.queryForObject(
            sql,
            Map.of(
                "organizacaoId", organizacaoId,
                "ano", ano,
                "atualizadoEm",
                OffsetDateTime.now(ZoneOffset.UTC)
            ),
            Long.class
        );

        if (numero == null) {
            throw new IllegalStateException(
                "Nao foi possivel gerar o numero do contrato"
            );
        }

        return numero;
    }
}
