package br.com.caisora.dbfirst;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita os 15 mappers gerados no PostgreSQL real; fixtures sao manuais. */
class AllTablesTest {
    private static final Map<String, String> MODELS = Map.ofEntries(
            Map.entry("clientes", "Cliente"),
            Map.entry("contratos", "Contrato"),
            Map.entry("embarcacoes", "Embarcacao"),
            Map.entry("historicos_contrato", "HistoricoContrato"),
            Map.entry("historicos_movimentacoes", "HistoricoMovimentacao"),
            Map.entry("movimentacoes", "Movimentacao"),
            Map.entry("ocupacoes", "Ocupacao"),
            Map.entry("organizacao_modulos", "OrganizacaoModulo"),
            Map.entry("organizacoes", "Organizacao"),
            Map.entry("perfil_permissoes", "PerfilPermissao"),
            Map.entry("posicoes_embarcacoes", "PosicaoEmbarcacao"),
            Map.entry("sequencias_contrato", "SequenciaContrato"),
            Map.entry("usuarios", "Usuario"),
            Map.entry("vagas", "Vaga"),
            Map.entry("vinculos_contrato_ocupacao", "VinculoContratoOcupacao")
    );
    private static SqlSessionFactory factory;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-13T15:00:00Z");
    @BeforeAll static void connect() throws Exception { factory = Database.factory(); }
    static Stream<String> tables() { return MODELS.keySet().stream().sorted(); }
    static String namespace(String table) { return "br.com.caisora.dbfirst.generated.mapper." + MODELS.get(table) + "Mapper."; }
    static String property(String column) {
        var parts = column.split("_"); var result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        return result.toString();
    }

    @Test void generationCoversEveryBusinessTableColumnPrimaryKeyAndForeignKey() throws Exception {
        var report = new StringBuilder();
        try (var connection = Database.dataSource().getConnection()) {
            Database.guard(connection); var metadata = connection.getMetaData(); var actual = new TreeSet<String>();
            try (var rows = metadata.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rows.next()) if (!Set.of("flyway_schema_history", "database_first_demo_guard").contains(rows.getString("TABLE_NAME"))) actual.add(rows.getString("TABLE_NAME"));
            }
            assertEquals(new TreeSet<>(MODELS.keySet()), actual, "Nenhuma tabela de negocio pode ficar sem geracao.");
            for (var table : actual) {
                String modelName = MODELS.get(table), ns = namespace(table);
                var type = Class.forName("br.com.caisora.dbfirst.generated.model." + modelName);
                var xml = Files.readString(Path.of("generated/resources/" + ns.substring(0, ns.length()-1).replace('.', '/') + ".xml"));
                report.append("TABLE ").append(table).append(" -> ").append(modelName).append('\n');
                int columns = 0;
                try (var rows = metadata.getColumns(null, "public", table, null)) {
                    while (rows.next()) {
                        String column = rows.getString("COLUMN_NAME"), sqlType = rows.getString("TYPE_NAME");
                        var field = type.getDeclaredField(property(column)); columns++;
                        assertTrue(xml.contains("column=\"" + column + "\""), column);
                        if (sqlType.equals("uuid")) assertEquals(UUID.class, field.getType());
                        if (sqlType.equals("timestamptz")) assertEquals(OffsetDateTime.class, field.getType());
                        if (sqlType.equals("date")) assertEquals(LocalDate.class, field.getType());
                        if (sqlType.equals("numeric")) assertEquals(BigDecimal.class, field.getType());
                        if (sqlType.equals("jsonb")) assertEquals(String.class, field.getType());
                        report.append("  COLUMN ").append(column).append(' ').append(sqlType).append(" -> ").append(field.getType().getSimpleName()).append('\n');
                    }
                }
                assertEquals(columns, type.getDeclaredFields().length);
                try (var rows = metadata.getPrimaryKeys(null, "public", table)) {
                    while (rows.next()) report.append("  PK ").append(rows.getString("COLUMN_NAME")).append('\n');
                }
                try (var rows = metadata.getImportedKeys(null, "public", table)) {
                    while (rows.next()) report.append("  FK ").append(rows.getString("FKCOLUMN_NAME")).append(" -> ").append(rows.getString("PKTABLE_NAME")).append('.').append(rows.getString("PKCOLUMN_NAME")).append('\n');
                }
                for (var operation : List.of("insert", "selectAll", "selectByPrimaryKey", "updateByPrimaryKey", "deleteByPrimaryKey")) assertTrue(factory.getConfiguration().hasStatement(ns + operation), ns + operation);
            }
        }
        Files.writeString(Path.of("target/all-tables-metadata.txt"), report);
    }

    @ParameterizedTest(name="{0}: persistencia gerada") @MethodSource("tables")
    void generatedPersistenceWorksForEveryTable(String table) throws Exception {
        // Rollback integral evita deixar cadastros ou historicos de teste na demonstracao.
        try (var session = factory.openSession()) {
            var fixtures = new Fixtures(session); Object input = fixtures.insert(table), key = fixtures.key(table);
            String ns = namespace(table);
            Object found = session.selectOne(ns + "selectByPrimaryKey", key);
            assertNotNull(found); fixtures.compare(input, found);
            assertTrue(session.selectList(ns + "selectAll").stream().anyMatch(row -> fixtures.sameKey(table, row)));
            if (table.equals("historicos_movimentacoes")) {
                var savepoint = session.getConnection().setSavepoint();
                DatabaseFirstTest.state("55000", () -> session.update(ns + "updateByPrimaryKey", input));
                session.getConnection().rollback(savepoint); session.clearCache();
                savepoint = session.getConnection().setSavepoint();
                DatabaseFirstTest.state("55000", () -> session.delete(ns + "deleteByPrimaryKey", key));
                session.getConnection().rollback(savepoint); session.clearCache();
                assertNotNull(session.selectOne(ns + "selectByPrimaryKey", key));
            } else {
                String field = switch (table) {
                    case "organizacoes", "usuarios", "clientes", "embarcacoes" -> "nome";
                    case "organizacao_modulos" -> "ativo";
                    case "perfil_permissoes" -> "permitido";
                    case "sequencias_contrato" -> "ultimoNumero";
                    case "contratos" -> "valorBase";
                    case "posicoes_embarcacoes" -> "descricaoLocal";
                    case "historicos_contrato" -> "descricao";
                    case "vinculos_contrato_ocupacao" -> "motivoFim";
                    default -> "observacoes";
                };
                Object value = switch (field) {
                    case "ativo", "permitido" -> false;
                    case "ultimoNumero" -> 2L;
                    case "valorBase" -> new BigDecimal("1800.50");
                    default -> "Atualizado pelo teste";
                };
                SystemMetaObject.forObject(input).setValue(field, value);
                assertEquals(1, session.update(ns + "updateByPrimaryKey", input));
                fixtures.compare(input, session.selectOne(ns + "selectByPrimaryKey", key));
                assertEquals(1, session.delete(ns + "deleteByPrimaryKey", key));
                assertNull(session.selectOne(ns + "selectByPrimaryKey", key));
            }
            session.rollback();
        }
    }

    private static final class Fixtures {
        final SqlSession session;
        final Map<String, UUID> ids = new HashMap<>();
        final Map<String, Object> inserted = new HashMap<>();
        Fixtures(SqlSession session) { this.session = session; MODELS.keySet().forEach(table -> ids.put(table, UUID.randomUUID())); }
        Object key(String table) { return table.equals("sequencias_contrato") ? Map.of("organizacaoId", ids.get("organizacoes"), "ano", 2026) : ids.get(table); }
        boolean sameKey(String table, Object row) {
            var bean = SystemMetaObject.forObject(row);
            return table.equals("sequencias_contrato") ? ids.get("organizacoes").equals(bean.getValue("organizacaoId")) && Integer.valueOf(2026).equals(bean.getValue("ano")) : ids.get(table).equals(bean.getValue("id"));
        }
        void compare(Object expected, Object actual) throws Exception {
            assertNotNull(actual); assertEquals(expected.getClass(), actual.getClass());
            var left = SystemMetaObject.forObject(expected); var right = SystemMetaObject.forObject(actual);
            for (var field : expected.getClass().getDeclaredFields()) {
                Object a = left.getValue(field.getName()), b = right.getValue(field.getName());
                if (field.getName().equals("dadosAnteriores") || field.getName().equals("dadosNovos")) {
                    assertEquals(a == null ? null : JSON.readTree((String) a), b == null ? null : JSON.readTree((String) b));
                } else assertEquals(a, b, field.getName());
            }
        }
        void dependency(String... tables) throws Exception { for (String table : tables) insert(table); }
        Object insert(String table) throws Exception {
            if (inserted.containsKey(table)) return inserted.get(table);
            if (!table.equals("organizacoes")) dependency("organizacoes");
            switch (table) {
                case "embarcacoes" -> dependency("clientes");
                case "ocupacoes", "posicoes_embarcacoes" -> dependency("embarcacoes", "vagas");
                case "movimentacoes" -> dependency("embarcacoes", "vagas", "usuarios");
                case "historicos_movimentacoes" -> dependency("movimentacoes", "usuarios");
                case "contratos" -> dependency("clientes", "embarcacoes", "usuarios");
                case "historicos_contrato" -> dependency("contratos", "usuarios");
                case "vinculos_contrato_ocupacao" -> dependency("contratos", "ocupacoes");
                default -> { }
            }
            var fields = new LinkedHashMap<String, Object>();
            fields.put("id", ids.get(table)); fields.put("organizacaoId", ids.get("organizacoes"));
            fields.put("criadoEm", NOW); fields.put("criadaEm", NOW); fields.put("atualizadoEm", NOW); fields.put("atualizadaEm", NOW);
            fields.put("ativo", true); fields.put("ativa", true); fields.put("versao", 0L);
            switch (table) {
                case "organizacoes" -> {
                    fields.put("nome", "Marina de validacao"); fields.put("email", "validacao@example.invalid");
                    fields.put("slug", "teste-" + ids.get(table)); fields.put("latitude", new BigDecimal("-23.550000")); fields.put("longitude", new BigDecimal("-46.630000"));
                }
                case "usuarios" -> {
                    fields.put("nome", "Operador local"); fields.put("email", "operador@example.invalid");
                    fields.put("senhaHash", "nao-utilizado-para-autenticacao"); fields.put("perfil", "GERENTE");
                }
                case "clientes" -> { fields.put("nome", "Cliente local"); fields.put("tipoPessoa", "FISICA"); fields.put("cpfCnpj", "12345678901"); }
                case "embarcacoes" -> {
                    fields.put("nome", "Aurora"); fields.put("proprietarioId", ids.get("clientes"));
                    fields.put("tipo", "LANCHA"); fields.put("tipoPropulsao", "MOTOR"); fields.put("codigoPaisBandeira", "BR");
                    fields.put("comprimentoTotalMetros", new BigDecimal("8.50")); fields.put("bocaMetros", new BigDecimal("2.50"));
                }
                case "vagas" -> {
                    fields.put("codigo", "A-01"); fields.put("tipo", "SECA"); fields.put("possuiAgua", true); fields.put("possuiEnergia", false);
                    fields.put("comprimentoMaximoMetros", new BigDecimal("12.00")); fields.put("bocaMaximaMetros", new BigDecimal("4.00"));
                }
                case "ocupacoes" -> {
                    fields.put("embarcacaoId", ids.get("embarcacoes")); fields.put("vagaId", ids.get("vagas"));
                    fields.put("status", "ATIVA"); fields.put("inicioEm", NOW);
                }
                case "posicoes_embarcacoes" -> { fields.put("embarcacaoId", ids.get("embarcacoes")); fields.put("tipo", "VAGA"); fields.put("vagaId", ids.get("vagas")); }
                case "movimentacoes" -> {
                    fields.put("embarcacaoId", ids.get("embarcacoes")); fields.put("tipo", "LANCAMENTO"); fields.put("status", "AGENDADA"); fields.put("prioridade", "NORMAL");
                    fields.put("tipoPosicaoOrigem", "VAGA"); fields.put("vagaOrigemId", ids.get("vagas")); fields.put("tipoPosicaoDestino", "AGUA");
                    fields.put("agendadaPara", NOW); fields.put("solicitadaPorId", ids.get("usuarios"));
                }
                case "historicos_movimentacoes" -> {
                    fields.put("movimentacaoId", ids.get("movimentacoes")); fields.put("usuarioId", ids.get("usuarios")); fields.put("ocorridoEm", NOW);
                    fields.put("tipoEvento", "ATUALIZADA"); fields.put("statusAnterior", "AGENDADA"); fields.put("statusNovo", "AGENDADA");
                    fields.put("dadosAnteriores", "{\"prioridade\":\"NORMAL\"}"); fields.put("dadosNovos", "{\"prioridade\":\"ALTA\"}");
                }
                case "contratos" -> {
                    fields.put("numero", "CTR-2026-001"); fields.put("clienteId", ids.get("clientes")); fields.put("embarcacaoId", ids.get("embarcacoes")); fields.put("criadoPorId", ids.get("usuarios"));
                    fields.put("status", "RASCUNHO"); fields.put("tipoVagaContratada", "SECA"); fields.put("periodicidade", "MENSAL");
                    fields.put("dataInicio", LocalDate.of(2026, 11, 13)); fields.put("renovacaoAutomatica", false); fields.put("valorBase", new BigDecimal("1500.00")); fields.put("diaVencimento", 10);
                }
                case "sequencias_contrato" -> { fields.put("ano", 2026); fields.put("ultimoNumero", 1L); }
                case "vinculos_contrato_ocupacao" -> { fields.put("contratoId", ids.get("contratos")); fields.put("ocupacaoId", ids.get("ocupacoes")); fields.put("inicioEm", NOW); }
                case "historicos_contrato" -> {
                    fields.put("contratoId", ids.get("contratos")); fields.put("tipoEvento", "CRIADO"); fields.put("statusNovo", "RASCUNHO");
                    fields.put("realizadoPorId", ids.get("usuarios")); fields.put("realizadoEm", NOW);
                }
                case "organizacao_modulos" -> fields.put("modulo", "CLIENTES");
                case "perfil_permissoes" -> { fields.put("modulo", "CLIENTES"); fields.put("perfil", "ATENDENTE"); fields.put("acao", "VISUALIZAR"); fields.put("permitido", true); }
                default -> throw new IllegalArgumentException(table);
            }
            Object row = Class.forName("br.com.caisora.dbfirst.generated.model." + MODELS.get(table)).getConstructor().newInstance();
            var bean = SystemMetaObject.forObject(row);
            for (var entry : fields.entrySet()) if (bean.hasSetter(entry.getKey())) bean.setValue(entry.getKey(), entry.getValue());
            assertEquals(1, session.insert(namespace(table) + "insert", row));
            inserted.put(table, row); return row;
        }
    }
}
