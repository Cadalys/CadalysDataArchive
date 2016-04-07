package com.cadalys.heroku.utils;

import com.cadalys.heroku.archive.ArchiveService;

/**
 * Created by dzmitrykalachou on 19.12.15.
 */
public class DBUtils {


    /**
     * Util method that generate sql script to create archive trigger for specified table
     *
     * @param schema
     * @param table
     * @return sql script
     */
    public static String getArchiveTriggerStatement(String schema, String table) {
        return String.format("CREATE OR REPLACE FUNCTION archive_procedure_%1$s()\n" +
                "    RETURNS trigger AS\n" +
                "            $BODY$\n" +
                "    BEGIN\n" +
                "    EXECUTE 'INSERT INTO %2$s.a__%1$s(' || ARRAY_TO_STRING(ARRAY(SELECT COLUMN_NAME :: VARCHAR(50)\n" +
                "        FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "        WHERE\n" +
                "        TABLE_NAME = '%1$s' AND\n" +
                "        table_schema = '%2$s'\n" +
                "        ORDER BY ORDINAL_POSITION\n" +
                "        ), ', ') || ')' || ' SELECT $1.' ||\n" +
                "        ARRAY_TO_STRING(ARRAY(SELECT COLUMN_NAME :: VARCHAR(50)\n" +
                "        FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "        WHERE\n" +
                "        TABLE_NAME = '%1$s' AND table_schema = '%2$s'\n" +
                "        ORDER BY ORDINAL_POSITION\n" +
                "        ), ', $1.')\n" +
                "        USING OLD;" +
                "    RETURN OLD;\n" +
                "    END;\n" +
                "    $BODY$\n" +
                "    LANGUAGE plpgsql;\n" +
                "    DROP TRIGGER IF EXISTS a__%1$s_delete ON %2$s.%1$s;\n" +
                "    CREATE TRIGGER a__%1$s_delete BEFORE DELETE ON %2$s.%1$s FOR EACH ROW EXECUTE PROCEDURE " +
                "archive_procedure_%1$s();\n", table, schema);

    }


    /**
     * Util method that generate sql for trigger that fill ParentExtId with value of refField
     *
     * @param schema
     * @param table
     * @param refField
     * @param isAttachment
     * @return
     */
    public static String getUpdateParentExtIdTriggerStatement(String schema, String table, String refField,
            boolean isAttachment) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE OR REPLACE FUNCTION update_parent_ext_id_procedure_%1$s()\n" +
                "                    RETURNS trigger AS\n" +
                "                            $BODY$\n" +
                "                    BEGIN\n");

        if (isAttachment) {
            buildParentExtColumnLinkWithRefColumn(refField, sqlBuilder, ArchiveService.PARENT_EXT_ID_1_COLUMN);
            buildParentExtColumnLinkWithRefColumn(refField, sqlBuilder, ArchiveService.PARENT_EXT_ID_2_COLUMN);
            buildParentExtColumnLinkWithRefColumn(refField, sqlBuilder, ArchiveService.PARENT_EXT_ID_3_COLUMN);
        } else {
            buildParentExtColumnLinkWithRefColumn(refField, sqlBuilder, ArchiveService.PARENT_EXT_ID_COLUMN);
        }


        sqlBuilder.append(" RETURN NEW;\n" +
                "                    END;\n" +
                "                    $BODY$\n" +
                "                    LANGUAGE plpgsql;\n" +
                "                    DROP TRIGGER IF EXISTS a__%1$s_insert ON %2$s.a__%1$s;\n" +
                "                    CREATE TRIGGER a__%1$s_insert BEFORE INSERT ON %2$s.a__%1$s FOR EACH ROW EXECUTE " +
                "PROCEDURE update_parent_ext_id_procedure_%1$s();\n");


        return String.format(sqlBuilder.toString(), table, schema);

    }

    private static void buildParentExtColumnLinkWithRefColumn(String refField, StringBuilder sqlBuilder,
            String parentExtIdColumn) {
        sqlBuilder.append("NEW.");
        sqlBuilder.append(parentExtIdColumn);
        sqlBuilder.append("=NEW.");
        sqlBuilder.append(refField);
        sqlBuilder.append(";\n");
    }

    /**
     * Util method that generate sql script to delete archive trigger for specified table
     *
     * @param schema
     * @param table
     * @return sql script
     */
    public static String deleteTriggerStatement(String schema, String table) {
        return String.format("DROP TRIGGER IF EXISTS a__%1$s_delete ON %2$s.%1$s;", table, schema);
    }


}