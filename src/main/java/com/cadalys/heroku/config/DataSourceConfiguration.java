package com.cadalys.heroku.config;

import org.postgresql.ds.PGPoolingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by dzmitrykalachou on 23.12.15.
 */
@Configuration
public class DataSourceConfiguration {

    public static final String SCHEMA_NAME = "salesforce";

    private static final String DATABASE_URL_PROPERTY = "DATABASE_URL";
    private static final String DATABASE_SSL_PROPERTY = "DATABASE_SSL";
    private static final String SSL_FACTORY = "org.postgresql.ssl.NonValidatingFactory";

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        URI postgresUri = new URI(System.getenv(DATABASE_URL_PROPERTY));
        String[] userInfoParts = postgresUri.getUserInfo().split(":");
        final String username = userInfoParts[0];
        final String password = userInfoParts[1];
        final String databaseName = postgresUri.getPath().substring(1);

        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setServerName(postgresUri.getHost());
        dataSource.setDatabaseName(databaseName);
        dataSource.setPortNumber(postgresUri.getPort());
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setCurrentSchema(SCHEMA_NAME);

        if (System.getenv(DATABASE_SSL_PROPERTY) != null) {
            dataSource.setSsl(true);
            dataSource.setSslfactory(SSL_FACTORY);
        }

        return dataSource;
    }


}
