package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.settings.DatabaseSettings;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.NotNull;


import java.util.UUID;

public class MariaDatabase {

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load mariadb driver");
        }
    }

    public static SqlSessionFactory buildSessionFactory(@NotNull DatabaseSettings settings) {
        String url = "jdbc:" + settings.url();
        url += url.contains("?") ? "&autoReconnect=true" : "?autoReconnect=true";
        PooledDataSource dataSource = new PooledDataSource("org.mariadb.jdbc.Driver", url, settings.username(), settings.password());
        dataSource.setPoolPingEnabled(true);
        dataSource.setPoolPingQuery("SELECT 1");
        dataSource.setPoolPingConnectionsNotUsedFor(600_000);
        Environment environment = new Environment("production", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.getTypeHandlerRegistry().register(UUID.class, JdbcType.OTHER, UUIDAsBin16Handler.class);
        configuration.addMapper(MariaChestshopMapper.class);
        configuration.addMapper(MariaPreferenceMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

}
