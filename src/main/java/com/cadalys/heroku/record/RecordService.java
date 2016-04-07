package com.cadalys.heroku.record;

import com.cadalys.heroku.config.DataSourceConfiguration;
import com.cadalys.heroku.exception.ArchiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dzmitrykalachou on 05.04.16.
 */
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
