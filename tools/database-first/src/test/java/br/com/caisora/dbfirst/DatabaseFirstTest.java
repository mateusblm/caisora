package br.com.caisora.dbfirst;

import br.com.caisora.dbfirst.generated.mapper.*;
import br.com.caisora.dbfirst.generated.model.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseFirstTest {
    static SqlSessionFactory factory;
    @BeforeAll static void connect() throws Exception { factory = Database.factory(); }

    static OrganizacaoModulo module(UUID id, UUID tenant, String label) {
        var row = new OrganizacaoModulo(); row.setId(id); row.setOrganizacaoId(tenant); row.setModulo(label); row.setAtivo(true); return row;
    }
    static PerfilPermissao permission(UUID id, UUID tenant, String label) {
        var row = new PerfilPermissao(); row.setId(id); row.setOrganizacaoId(tenant); row.setModulo(label);
        row.setPerfil("ATENDENTE"); row.setAcao("CONSULTAR"); row.setPermitido(true); return row;
    }
    static String name(String table) { return table.equals("modulos") ? "organizacao_modulos" : "perfil_permissoes"; }
    static void insert(String table, UUID id, UUID tenant, String label) {
        try (var session = factory.openSession()) {
            if (table.equals("modulos")) session.getMapper(OrganizacaoModuloMapper.class).insert(module(id, tenant, label));
            else session.getMapper(PerfilPermissaoMapper.class).insert(permission(id, tenant, label));
            session.commit();
        }
    }
    static void cleanup(String table, UUID id) {
        try (var session = factory.openSession()) {
            if (table.equals("modulos")) session.getMapper(OrganizacaoModuloMapper.class).deleteByPrimaryKey(id);
            else session.getMapper(PerfilPermissaoMapper.class).deleteByPrimaryKey(id);
            session.commit();
        }
    }
    static void state(String expected, org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(RuntimeException.class, action);
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) { assertEquals(expected, sql.getSQLState()); return; }
        }
        fail("Excecao nao contem SQLState: " + error);
    }
    static boolean persistedFlag(String table, UUID id) throws Exception {
        try (var connection = Database.dataSource().getConnection(); var query = connection.prepareStatement(
                "select " + (table.equals("modulos") ? "ativo" : "permitido") + " from " + name(table) + " where id = ?")) {
            query.setObject(1, id); try (var rs = query.executeQuery()) { assertTrue(rs.next()); return rs.getBoolean(1); }
        }
    }
    static void absent(String table, UUID id) throws Exception {
        try (var connection = Database.dataSource().getConnection(); var query = connection.prepareStatement("select count(*) from " + name(table) + " where id = ?")) {
            query.setObject(1, id); try (var rs = query.executeQuery()) { rs.next(); assertEquals(0, rs.getInt(1)); }
        }
    }

    @Test void postgresMetadataAndGeneratedFiles() throws Exception {
        var evidence = new StringBuilder();
        try (var connection = Database.dataSource().getConnection()) {
            Database.guard(connection); var meta = connection.getMetaData();
            evidence.append(meta.getDatabaseProductName()).append(' ').append(meta.getDatabaseProductVersion()).append('\n');
            assertEquals("PostgreSQL", meta.getDatabaseProductName());
            for (String table : List.of("organizacao_modulos", "perfil_permissoes")) {
                evidence.append("TABLE ").append(table).append('\n'); int columns = 0;
                try (var rows = meta.getColumns(null, "public", table, null)) {
                    while (rows.next()) { columns++; evidence.append("COLUMN ").append(rows.getString("COLUMN_NAME")).append(' ').append(rows.getString("TYPE_NAME")).append(" nullable=").append(rows.getInt("NULLABLE")).append('\n'); }
                }
                assertEquals(table.equals("organizacao_modulos") ? 4 : 6, columns);
                try (var rows = meta.getPrimaryKeys(null, "public", table)) { assertTrue(rows.next()); assertEquals("id", rows.getString("COLUMN_NAME")); evidence.append("PK id\n"); }
                try (var rows = meta.getImportedKeys(null, "public", table)) {
                    assertTrue(rows.next()); assertEquals("organizacao_id", rows.getString("FKCOLUMN_NAME")); assertEquals("organizacoes", rows.getString("PKTABLE_NAME"));
                    evidence.append("FK organizacao_id -> organizacoes.id\n");
                }
            }
        }
        Files.createDirectories(Path.of("target")); Files.writeString(Path.of("target/schema-metadata.txt"), evidence);
        for (String model : List.of("OrganizacaoModulo", "PerfilPermissao")) {
            var javaFile = Path.of("generated/java/br/com/caisora/dbfirst/generated/model/" + model + ".java");
            assertTrue(Files.readString(javaFile).contains("generated by MyBatis Generator"));
            var xml = Files.readString(Path.of("generated/resources/br/com/caisora/dbfirst/generated/mapper/" + model + "Mapper.xml"));
            for (String operation : List.of("insert", "selectByPrimaryKey", "updateByPrimaryKey", "deleteByPrimaryKey")) assertTrue(xml.contains("id=\"" + operation + "\""));
        }
    }

    @ParameterizedTest @ValueSource(strings={"modulos", "permissoes"})
    void generatedCrudCommitsAndCanBeReadFromIndependentConnection(String table) throws Exception {
        UUID id = UUID.randomUUID(); String label = "crud-" + id;
        try {
            insert(table, id, Database.TENANT, label); assertTrue(persistedFlag(table, id));
            try (var session = factory.openSession()) {
                if (table.equals("modulos")) {
                    var mapper = session.getMapper(OrganizacaoModuloMapper.class); var row = mapper.selectByPrimaryKey(id);
                    assertEquals(label, row.getModulo()); assertTrue(mapper.selectAll().stream().anyMatch(r -> id.equals(r.getId())));
                    row.setAtivo(false); assertEquals(1, mapper.updateByPrimaryKey(row));
                } else {
                    var mapper = session.getMapper(PerfilPermissaoMapper.class); var row = mapper.selectByPrimaryKey(id);
                    assertEquals(label, row.getModulo()); assertTrue(mapper.selectAll().stream().anyMatch(r -> id.equals(r.getId())));
                    row.setPermitido(false); assertEquals(1, mapper.updateByPrimaryKey(row));
                }
                session.commit();
            }
            assertFalse(persistedFlag(table, id)); cleanup(table, id); absent(table, id);
        } finally { cleanup(table, id); }
    }

    @ParameterizedTest @ValueSource(strings={"modulos", "permissoes"})
    void databaseEnforcesPkUniqueFkNotNullAndVarcharLength(String table) throws Exception {
        UUID id = UUID.randomUUID(); String label = "constraint-" + id;
        try {
            insert(table, id, Database.TENANT, label);
            state("23505", () -> insert(table, id, Database.TENANT, "other"));
            state("23505", () -> insert(table, UUID.randomUUID(), Database.TENANT, label));
            state("23503", () -> insert(table, UUID.randomUUID(), UUID.randomUUID(), "missing-parent"));
            state("23502", () -> insert(table, UUID.randomUUID(), Database.TENANT, null));
            state("22001", () -> insert(table, UUID.randomUUID(), Database.TENANT, "x".repeat(51)));
            assertTrue(persistedFlag(table, id));
            try (var connection = Database.dataSource().getConnection(); var query = connection.prepareStatement("delete from organizacoes where id = ?")) {
                query.setObject(1, Database.TENANT); var error = assertThrows(SQLException.class, query::executeUpdate); assertEquals("23503", error.getSQLState());
            }
        } finally { cleanup(table, id); }
    }

    @ParameterizedTest @ValueSource(strings={"modulos", "permissoes"})
    void integrationDoesNotExposeOrMutateAnotherTenant(String table) {
        UUID id = UUID.randomUUID(); var service = new CrudService(factory);
        try {
            insert(table, id, Database.OTHER_TENANT, "isolated-" + id);
            assertFalse(service.list(table).stream().anyMatch(row -> row instanceof OrganizacaoModulo m ? id.equals(m.getId()) : id.equals(((PerfilPermissao) row).getId())));
            assertThrows(CrudService.NotFound.class, () -> service.get(table, id));
            var input = table.equals("modulos") ? new CrudService.Input("CLIENTES", true, null, null, null) : new CrudService.Input("CLIENTES", null, "ATENDENTE", "CONSULTAR", true);
            assertThrows(CrudService.NotFound.class, () -> service.save(table, id, input));
            assertThrows(CrudService.NotFound.class, () -> service.delete(table, id));
        } finally { cleanup(table, id); }
    }
}
