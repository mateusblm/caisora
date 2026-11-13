package br.com.caisora.dbfirst;

import br.com.caisora.dbfirst.generated.mapper.*;
import br.com.caisora.dbfirst.generated.model.*;
import java.util.*;
import org.apache.ibatis.session.SqlSessionFactory;

/** Servico manual: validacao/tenant/transacao. Todo SQL CRUD vem dos mappers MBG. */
public final class CrudService {
    public record Input(String modulo, Boolean ativo, String perfil, String acao, Boolean permitido) {}
    public static final class NotFound extends RuntimeException {}
    private final SqlSessionFactory factory;
    public CrudService(SqlSessionFactory factory) { this.factory = factory; }

    private static void text(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 50) {
            throw new IllegalArgumentException(field + " deve ter de 1 a 50 caracteres.");
        }
    }
    private static void validate(String table, Input input) {
        if (input == null) throw new IllegalArgumentException("Informe os dados do registro.");
        text(input.modulo(), "Modulo");
        if (table.equals("modulos")) {
            if (input.ativo() == null) throw new IllegalArgumentException("Informe ativo (true ou false).");
            if (input.perfil() != null || input.acao() != null || input.permitido() != null) throw new IllegalArgumentException("Campos de permissao nao pertencem a modulos.");
        } else {
            text(input.perfil(), "Perfil"); text(input.acao(), "Acao");
            if (input.permitido() == null) throw new IllegalArgumentException("Informe permitido (true ou false).");
            if (input.ativo() != null) throw new IllegalArgumentException("Ativo nao pertence a permissoes.");
        }
    }
    public static void table(String table) {
        if (!Set.of("modulos", "permissoes").contains(table)) throw new NotFound();
    }
    public List<?> list(String table) {
        table(table);
        try (var session = factory.openSession()) {
            // MyBatis3Simple gera selectAll; a demonstracao e pequena. Nunca retorna outro tenant.
            if (table.equals("modulos")) return session.getMapper(OrganizacaoModuloMapper.class).selectAll().stream()
                    .filter(row -> Database.TENANT.equals(row.getOrganizacaoId())).toList();
            return session.getMapper(PerfilPermissaoMapper.class).selectAll().stream()
                    .filter(row -> Database.TENANT.equals(row.getOrganizacaoId())).toList();
        }
    }
    public Object get(String table, UUID id) {
        table(table);
        try (var session = factory.openSession()) {
            if (table.equals("modulos")) {
                var row = session.getMapper(OrganizacaoModuloMapper.class).selectByPrimaryKey(id);
                if (row == null || !Database.TENANT.equals(row.getOrganizacaoId())) throw new NotFound();
                return row;
            }
            var row = session.getMapper(PerfilPermissaoMapper.class).selectByPrimaryKey(id);
            if (row == null || !Database.TENANT.equals(row.getOrganizacaoId())) throw new NotFound();
            return row;
        }
    }
    public Object save(String table, UUID id, Input input) {
        table(table); validate(table, input);
        boolean insert = id == null;
        try (var session = factory.openSession()) {
            Object result;
            if (table.equals("modulos")) {
                var mapper = session.getMapper(OrganizacaoModuloMapper.class);
                var row = insert ? new OrganizacaoModulo() : mapper.selectByPrimaryKey(id);
                if (!insert && (row == null || !Database.TENANT.equals(row.getOrganizacaoId()))) throw new NotFound();
                row.setId(insert ? UUID.randomUUID() : id); row.setOrganizacaoId(Database.TENANT);
                row.setModulo(input.modulo()); row.setAtivo(input.ativo());
                int count = insert ? mapper.insert(row) : mapper.updateByPrimaryKey(row);
                if (count != 1) throw new NotFound();
                result = mapper.selectByPrimaryKey(row.getId());
            } else {
                var mapper = session.getMapper(PerfilPermissaoMapper.class);
                var row = insert ? new PerfilPermissao() : mapper.selectByPrimaryKey(id);
                if (!insert && (row == null || !Database.TENANT.equals(row.getOrganizacaoId()))) throw new NotFound();
                row.setId(insert ? UUID.randomUUID() : id); row.setOrganizacaoId(Database.TENANT);
                row.setModulo(input.modulo()); row.setPerfil(input.perfil()); row.setAcao(input.acao()); row.setPermitido(input.permitido());
                int count = insert ? mapper.insert(row) : mapper.updateByPrimaryKey(row);
                if (count != 1) throw new NotFound();
                result = mapper.selectByPrimaryKey(row.getId());
            }
            session.commit(); return result;
        }
    }
    public void delete(String table, UUID id) {
        table(table);
        try (var session = factory.openSession()) {
            int count;
            if (table.equals("modulos")) {
                var mapper = session.getMapper(OrganizacaoModuloMapper.class);
                var row = mapper.selectByPrimaryKey(id);
                if (row == null || !Database.TENANT.equals(row.getOrganizacaoId())) throw new NotFound();
                count = mapper.deleteByPrimaryKey(id);
            } else {
                var mapper = session.getMapper(PerfilPermissaoMapper.class);
                var row = mapper.selectByPrimaryKey(id);
                if (row == null || !Database.TENANT.equals(row.getOrganizacaoId())) throw new NotFound();
                count = mapper.deleteByPrimaryKey(id);
            }
            if (count != 1) throw new NotFound();
            session.commit();
        }
    }
}
