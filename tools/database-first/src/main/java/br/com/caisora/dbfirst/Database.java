package br.com.caisora.dbfirst;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.postgresql.ds.PGSimpleDataSource;

/** Integracao manual; nenhuma classe do produto e carregada. */
public final class Database {
    public static final UUID TENANT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private Database() {}

    public static String env(String key, String fallback) {
        return System.getenv().getOrDefault(key, fallback);
    }

    public static PGSimpleDataSource dataSource() {
        var ds = new PGSimpleDataSource();
        String host = env("DBF_HOST", "localhost");
        if (!host.equals("postgres") && !host.equals("localhost") && !host.equals("127.0.0.1")) {
            throw new IllegalStateException("A demo aceita somente PostgreSQL local ou o servico postgres do Compose.");
        }
        ds.setServerNames(new String[]{host});
        ds.setPortNumbers(new int[]{Integer.parseInt(env("DBF_PORT", "55432"))});
        ds.setDatabaseName("caisora_database_first");
        ds.setUser("caisora_demo");
        ds.setPassword(env("DBF_PASSWORD", "somente-demo-local"));
        ds.setConnectTimeout(5);
        return ds;
    }

    public static void guard(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "select current_database(), current_user, finalidade from database_first_demo_guard where id = 1")) {
            if (!rows.next() || !"caisora_database_first".equals(rows.getString(1))
                    || !"caisora_demo".equals(rows.getString(2))
                    || !"caisora-database-first-local".equals(rows.getString(3))) {
                throw new IllegalStateException("Banco sem identificacao de demonstracao; operacao recusada.");
            }
        }
    }

    public static SqlSessionFactory factory() throws SQLException {
        var source = dataSource();
        try (var connection = source.getConnection()) { guard(connection); }
        var config = new Configuration(new Environment("database-first-local", new JdbcTransactionFactory(), source));
        config.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
        config.addMappers("br.com.caisora.dbfirst.generated.mapper");
        return new SqlSessionFactoryBuilder().build(config);
    }
}
