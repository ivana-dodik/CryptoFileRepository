package org.etf.unibl.repo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {

   private static final HikariConfig config = new HikariConfig();
   private static final HikariDataSource ds;

   static {
      config.setJdbcUrl("jdbc:mysql://localhost:3306/krz");
      config.setUsername("root");
      config.setPassword("ivana1");
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      ds = new HikariDataSource(config);
   }


   private DataSource() {
   }

   public static Connection getConnection() throws SQLException {
      return ds.getConnection();
   }
}
