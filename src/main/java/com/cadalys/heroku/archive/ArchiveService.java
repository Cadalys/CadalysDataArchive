package com.cadalys.heroku.archive;

import com.cadalys.heroku.config.DataSourceConfiguration;
import com.cadalys.heroku.exception.ArchiveException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.model.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cadalys.heroku.utils.DBUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;


/**
 * Created by dzmitrykalachou on 19.12.15.
 */

@Service
public class ArchiveService {

    public static final String ARCHIVE_TABLE_PREFIX = "a__";
    public static final String PARENT_EXT_ID_COLUMN = "ParentExtID";
    public static final String PARENT_EXT_ID_1_COLUMN = "ParentExtID1";
    public static final String PARENT_EXT_ID_2_COLUMN = "ParentExtID2";
    public static final String PARENT_EXT_ID_3_COLUMN = "ParentExtID3";
    private static final Logger LOGGER = Logger.getLogger(ArchiveService.class);
    private static final String ATTACHMENT_TABLE_NAME = "attachment";

    private static final String SFID = "sfid";

    private static final String SQL_SPACE = " ";

    private final CloneHelper cloneHelper = new CloneHelper();

    @Autowired
    private DataSource dataSource;

    /**
     * Delete triggers of specified objects
     *
     * @param objects array of object(table name)
     */
    public void deleteObjectsTrigger(ArchiveTableNames objects) {
        List<String> errors = new ArrayList<>();

        try (final Connection connection = dataSource.getConnection()) {
            final Platform platformInstance = PlatformFactory.createNewPlatformInstance(dataSource);
            final Database database = platformInstance.readModelFromDatabase(DataSourceConfiguration.SCHEMA_NAME,
                    false);
            final Table[] tables = database.getTables();
            connection.setAutoCommit(false);

            for (String objectName : objects.getObjects()) {
                try {
                    String sql = DBUtils.deleteTriggerStatement(DataSourceConfiguration.SCHEMA_NAME, objectName);
                    LOGGER.info(sql);
                    connection.createStatement().execute(sql);
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

    /**
     * Check archive tables and triggers of specified objects
     *
     * @param objects array of object(table name)
     */
    public void checkObjects(ArchiveObjects objects) {
        List<String> errors = new ArrayList<>();

        try (final Connection connection = dataSource.getConnection()) {
            final Platform platformInstance = PlatformFactory.createNewPlatformInstance(dataSource);
            final Database database = platformInstance.readModelFromDatabase(DataSourceConfiguration.SCHEMA_NAME,
                    false);
            final Table[] tables = database.getTables();
            connection.setAutoCommit(false);

            for (ArchiveObject object : objects.getObjects()) {
                try {
                    String sql = handleTableChanges(platformInstance, database, tables, object.getChildObj(),
                            object.getParentObj(), object.getRefField());
                    LOGGER.info(sql);
                    connection.createStatement().execute(sql);
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

    private String handleTableChanges(Platform platform, Database database, Table[] tables, String table,
            String parentObject, String refField) throws Exception {
        final String archiveTableName = ARCHIVE_TABLE_PREFIX + table;
        final Optional<Table> originalTableOptional = Arrays.stream(tables).filter(e -> e.getName().equals(table))
                .findFirst();
        if (!originalTableOptional.isPresent()) {
            throw new Exception(String.format("object %s doesn't exists", table));
        }
        final Table originalTable = originalTableOptional.get();
        final Optional<Table> archiveOptional = Arrays.stream(tables).filter(e -> e.getName().equals(archiveTableName))
                .findAny();
        final boolean isAttachmentTable = table.equalsIgnoreCase(ATTACHMENT_TABLE_NAME);
        final StringBuilder sqlBuilder = new StringBuilder();
        if (archiveOptional.isPresent()) {
            final Table archiveTable = archiveOptional.get();
            List<Column> originalTableColumns = new ArrayList<>(Arrays.asList(originalTable.getColumns()));
            List<Column> archiveTableColumns = new ArrayList<>(Arrays.asList(archiveTable.getColumns()));

            final Collection<Column> newColumns = CollectionUtils.removeAll(originalTableColumns, archiveTableColumns,
                    new Equator<Column>() {
                        @Override
                        public boolean equate(Column o1, Column o2) {
                            return o1.getName().equals(o2.getName());
                        }

                        @Override
                        public int hash(Column o) {
                            return o.getName().hashCode();
                        }
                    });

            for (Column originalTableColumn : newColumns) {
                final Column newColumn = cloneHelper.clone(originalTableColumn, true);
                newColumn.setRequired(false);
                newColumn.setAutoIncrement(false);
                archiveTable.addColumn(newColumn);
            }

            checkAndAddParentExtIdModel(parentObject, isAttachmentTable, archiveTable, archiveTableColumns);

        } else {
            final Table archiveTable = cloneHelper.clone(originalTable, false, false, null, true);
            archiveTable.setName(archiveTableName);
            final Column[] archiveTableColumns = archiveTable.getColumns();
            for (int i = 0; i < archiveTableColumns.length; i++) {
                final Column archiveTableColumn = archiveTableColumns[i];
                archiveTableColumn.setAutoIncrement(false);
                archiveTableColumn.setRequired(false);
                archiveTableColumn.setPrimaryKey(false);

                if (archiveTableColumn.getName().equalsIgnoreCase(SFID)) {
                    archiveTableColumn.setPrimaryKey(true);
                }
            }
            final Index[] uniqueIndices = archiveTable.getUniqueIndices();
            if (uniqueIndices != null && uniqueIndices.length > 0) {
                for (Index uniqueIndex : uniqueIndices) {
                    archiveTable.removeIndex(uniqueIndex);
                }
            }
            final Index[] nonUniqueIndices = archiveTable.getNonUniqueIndices();
            if (nonUniqueIndices != null && nonUniqueIndices.length > 0) {
                for (Index nonUniqueIndex : nonUniqueIndices) {
                    archiveTable.removeIndex(nonUniqueIndex);
                }
            }

            checkAndAddParentExtIdModel(parentObject, isAttachmentTable, archiveTable,
                    new ArrayList<>(Arrays.asList(archiveTableColumns)));

            database.addTable(archiveTable);
        }
        sqlBuilder.append(platform.getAlterTablesSql(database));
        sqlBuilder.append(SQL_SPACE);
        sqlBuilder.append(DBUtils.getArchiveTriggerStatement(DataSourceConfiguration.SCHEMA_NAME, table));
        sqlBuilder.append(SQL_SPACE);


        if (parentObject != null && !parentObject.isEmpty() && refField != null && !refField.isEmpty()) {
            sqlBuilder.append(
                    DBUtils.getUpdateParentExtIdTriggerStatement(DataSourceConfiguration.SCHEMA_NAME, table, refField,
                            isAttachmentTable));
            sqlBuilder.append(SQL_SPACE);
        }

        return sqlBuilder.toString();
    }

    private void checkAndAddParentExtIdModel(String parentObject, boolean isAttachmentTable, Table archiveTable,
            List<Column> archiveTableColumns) {
        if (parentObject != null && !parentObject.isEmpty()) {
            if (isAttachmentTable) {
                checkColumnExistsAndAddNew(archiveTable, archiveTableColumns, PARENT_EXT_ID_1_COLUMN);
                checkColumnExistsAndAddNew(archiveTable, archiveTableColumns, PARENT_EXT_ID_2_COLUMN);
                checkColumnExistsAndAddNew(archiveTable, archiveTableColumns, PARENT_EXT_ID_3_COLUMN);
            } else {
                checkColumnExistsAndAddNew(archiveTable, archiveTableColumns, PARENT_EXT_ID_COLUMN);
            }
        }
    }

    private void checkColumnExistsAndAddNew(Table archiveTable, List<Column> archiveTableColumns, String columnName) {
        if (!archiveTableColumns.stream().anyMatch(e -> e.getName().equalsIgnoreCase(columnName))) {
            archiveTable.addColumn(buildParentExtColumn(columnName));
        }
    }

    private Column buildParentExtColumn(String name) {
        Column column = new Column();
        column.setName(name);
        column.setType("varchar");
        column.setSize("18");
        return column;
    }


}