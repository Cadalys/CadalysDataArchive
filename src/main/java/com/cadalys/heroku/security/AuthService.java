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


public class AuthService {

    private static final String SERVICE_USER = "SERVICE_USER";
    private static final String SERVICE_PASSWORD = "SERVICE_PASSWORD";
    private static final String VALUE_COLUMN_NAME = "cadarch__text_value__c";

    private static final String GET_PARAM_SQL = "select cadarch__text_value__c from cadarch__archiving_setting__c where name=?";

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
            preparedStatement.setString(1, SERVICE_USER);
            final ResultSet usernameResult = preparedStatement.executeQuery();
            if(!usernameResult.next()){
                throw new SettingsException("username doesn't exists in settings table");
            }
            final String username = usernameResult.getString(VALUE_COLUMN_NAME);
            if (username == null || username.isEmpty()) {
                throw new SettingsException("username doesn't exists in settings table");
            }
            preparedStatement.setString(1, SERVICE_PASSWORD);
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
