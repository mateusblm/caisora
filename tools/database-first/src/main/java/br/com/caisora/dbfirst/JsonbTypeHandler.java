package br.com.caisora.dbfirst;

import java.sql.*;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/** Adaptacao manual de JSONB; os modelos, mappers e instrucoes SQL sao gerados. */
public final class JsonbTypeHandler extends BaseTypeHandler<String> {
    @Override public void setNonNullParameter(PreparedStatement ps, int i, String value, JdbcType type) throws SQLException {
        var json = new PGobject(); json.setType("jsonb"); json.setValue(value); ps.setObject(i, json);
    }
    @Override public String getNullableResult(ResultSet rs, String column) throws SQLException { return rs.getString(column); }
    @Override public String getNullableResult(ResultSet rs, int column) throws SQLException { return rs.getString(column); }
    @Override public String getNullableResult(CallableStatement cs, int column) throws SQLException { return cs.getString(column); }
}
