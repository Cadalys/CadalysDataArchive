package com.cadalys.heroku.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Base64Utils;
import com.cadalys.heroku.exception.ArchiveException;
import com.cadalys.heroku.exception.SettingsException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
public class AuthService {

    private static final String METADATA_SERVICE_USER = "METADATA_SERVICE_USER";
    private static final String METADATA_SERVICE_PASSWORD = "METADATA_SERVICE_PASSWORD";
    private static final String VALUE_COLUMN_NAME = "text_value__c";

    private static final String GET_PARAM_SQL = "select text_value__c from archiving_settings__c where name=?";

    @Autowired
    private DataSource dataSource;

    public AuthService() {
    }

    /**
     * Check authorization token
     *
     * @param token
     * @return
     */
    public boolean checkAuthorization(String token) {
        try (final Connection connection = dataSource.getConnection()){
            final PreparedStatement preparedStatement = connection.prepareStatement(GET_PARAM_SQL);
            preparedStatement.setString(1, METADATA_SERVICE_USER);
            final ResultSet usernameResult = preparedStatement.executeQuery();
            if(!usernameResult.next()){
                throw new SettingsException("username doesn't exists in settings table");
            }
            final String username = usernameResult.getString(VALUE_COLUMN_NAME);
            if (username == null || username.isEmpty()) {
                throw new SettingsException("username doesn't exists in settings table");
            }
            preparedStatement.setString(1, METADATA_SERVICE_PASSWORD);
            final ResultSet passwordResult = preparedStatement.executeQuery();
            if(!passwordResult.next()){
                throw new SettingsException("password doesn't exists in settings table");
            }
            final String password = passwordResult.getString(VALUE_COLUMN_NAME);
            if (password == null || password.isEmpty()) {
                throw new SettingsException("password doesn't exists in settings table");
            }

            return token.equals(buildBase64Token(username, password));

        } catch (SQLException e) {
            throw new ArchiveException(Arrays.asList(e.getMessage()));
        }
    }

    private String buildBase64Token(String username, String password) {
        return "Basic " + Base64Utils.encodeToString(new String(username + ":" + password).getBytes());
    }


}
