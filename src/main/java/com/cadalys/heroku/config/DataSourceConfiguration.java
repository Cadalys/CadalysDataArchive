/**
 *  Copyright (c), Cadalys, Inc.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *     are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *  - Neither the name of the Cadalys, Inc., nor the names of its contributors
 *      may be used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 *  OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.cadalys.heroku.config;

import org.postgresql.ds.PGPoolingDataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration class to initialize MVC part of the application
 * <p/>
 */
@Configuration
@ComponentScan(basePackages = {"com.cadalys.heroku.stereotype"})
@PropertySource("classpath:application.properties")
@EnableWebMvc
@EnableAsync
public class DataSourceConfiguration extends WebMvcConfigurerAdapter {

    public static final String SCHEMA_NAME = "salesforce";

    private static final String DATABASE_URL_PROPERTY = "DATABASE_URL";
    private static final String DATABASE_SSL_PROPERTY = "DATABASE_SSL";
    private static final String SSL_FACTORY = "org.postgresql.ssl.NonValidatingFactory";

    @Autowired
    private Environment env;

    /**
     * Adds CORS support
     * <p/>
     * Note: by default all origins are allowed, there was no specific requirements addressing CORS
     *
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**");
    }

    /**
     * Initializing  datasource using application properties
     *
     * @return
     */
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


    /**
     * Initialises transaction manager for the datasource in context
     *
     * @return
     */
    @Bean
    public DataSourceTransactionManager txManager() throws URISyntaxException {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager();
        txManager.setDataSource(dataSource());
        return txManager;
    }

    /**
     * Initializes instance of JDBCTemplate, which is thread-safe by default and may be referenced across
     * different beans
     *
     * @return
     */
    @Bean
    public JdbcTemplate jdbcTemplate() throws URISyntaxException {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        return jdbcTemplate;
    }

    /**
     * Gson serialization, this is where all custom type serializers may be used. The gson
     * is used within the application to aid JSON transformation. The application has no
     * strict data model and all the REST service requests and response may be changed any time
     * without affecting too many classes.
     *
     * @return
     */
    @Bean
    public Gson gson() {
        return new GsonBuilder().create();
    }

}
