package com.cadalys.heroku.stereotype.service;

import com.cadalys.heroku.config.DataSourceConfiguration;
import com.cadalys.heroku.exception.ArchiveStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArchiveStatusService {

    private static final String ARCHIVE_TABLE_NAME = "cadarch__archive_plan__c";

    private static final String SELECT_ROW_STATEMENT =
            "select %1$s." + ColumnSelector.RECORD_ID.columnName + ", " +
                    "%1$s." + ColumnSelector.TIMESTAMP.columnName +
                    " from " + DataSourceConfiguration.SCHEMA_NAME + ".%1$s" +
                    " where %1$s." + ColumnSelector.RECORD_ID.columnName + " in (%2$s);";

    @Autowired
    private DataSource dataSource;

    public List<Map<String, Object>> findByIds(final String[] identifiers) {
        final List<String> errors = new ArrayList<>();

        final List<Map<String, Object>> records = new ArrayList<>();

        try {
            final String questionMarks = String.join(", ", Collections.nCopies(identifiers.length, "?"));
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(
                         String.format(SELECT_ROW_STATEMENT, ARCHIVE_TABLE_NAME, questionMarks))) {
                for (int i = 0; i < identifiers.length; i++) {
                    statement.setString(i + 1, identifiers[i]);
                }

                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final Map<String, Object> record = new HashMap<>();
                    record.put(ColumnSelector.RECORD_ID.resultColumnName,
                            resultSet.getString(ColumnSelector.RECORD_ID.columnName));
                    record.put(ColumnSelector.TIMESTAMP.resultColumnName,
                            Optional.ofNullable(resultSet.getTimestamp(ColumnSelector.TIMESTAMP.columnName))
                                    .map(Timestamp::getTime).orElse(null));

                    records.add(record);
                }
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new ArchiveStatusException(errors);
        }

        return records;
    }

    private enum ColumnSelector {
        RECORD_ID("sfid", "recordId"),
        TIMESTAMP("lastmodifieddate", "timestamp");

        ColumnSelector(final String columnName, final String resultColumnName) {
            this.columnName = columnName;
            this.resultColumnName = resultColumnName;
        }

        private final String columnName;

        private final String resultColumnName;
    }
}
