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
package com.cadalys.heroku.stereotype.service;

import com.cadalys.heroku.record.RecordObject;
import com.cadalys.heroku.record.RecordObjects;
import com.cadalys.heroku.config.DataSourceConfiguration;
import com.cadalys.heroku.exception.ArchiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecordService {


    private static final String DELETE_ROW_STATEMENT =
            "delete from " + DataSourceConfiguration.SCHEMA_NAME + ".%1$s where %1$s.sfid=?";

    @Autowired
    private DataSource dataSource;

    /**
     * Delete records by object name and sfid
     *
     * @param objects
     */
    public void deleteRecords(RecordObjects objects) {
        List<String> errors = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (RecordObject object : objects.getObjects()) {
                try {
                    final PreparedStatement deletePreparedStatement = connection.prepareStatement(
                            String.format(DELETE_ROW_STATEMENT, object.getObject()));
                    deletePreparedStatement.setString(1, object.getId());
                    if (deletePreparedStatement.executeUpdate() < 1) {
                        errors.add(String.format("row of object %s with sfid=%s wasn't delete. Row doesn't exists",
                                object.getObject(), object.getId()));
                    }
                    connection.commit();
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new ArchiveException(errors);
        }
    }

}
