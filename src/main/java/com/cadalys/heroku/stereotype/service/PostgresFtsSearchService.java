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

import com.google.gson.Gson;
import com.cadalys.heroku.Field;
import com.cadalys.heroku.ProxySQLOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostgresFtsSearchService implements FtsSearchService {

    private static final String SQL_INJECT_CHECK_CREATE = "\\b(CREATE)\\b";
    private static final String SQL_INJECT_CHECK_DROP = "\\b(DROP)\\b";
    private static final String SQL_INJECT_CHECK = "(;)|(\\b(ALTER|DELETE|DROP|EXEC(UTE){0,1}|INSERT( +INTO){0,1}|MERGE|SELECT|UPDATE|UNION( +ALL){0,1})\\b)";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Gson gson;

    @Override
    public String createTsvectorIndex(final Map<String, Object> request) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String ftsConfig = readFtsConfigurationParam(request);
        Map<String,Object> tableDescr = (Map<String,Object>)request.get(Field.TABLE);
        sb.append(request.get(Field.NAME)).append(" ON ")
                .append(tableDescr.get(Field.NAME)).append(" USING ").append(readTsvectorType(request))
                .append(" (to_tsvector('").append(readFtsConfigurationParam(request)).append("',")
                .append(makeToTsvectorStatement((List<Map<String, Object>>) tableDescr.get(Field.COLUMNS))).append("))");
        checkForSQLInjectionAttack(sb.toString(), ProxySQLOperation.CREATE);
        sb.insert(0, "CREATE INDEX ");
        log.debug("creating index via SQL: {}", sb.toString());
        jdbcTemplate.execute(sb.toString());
        return sb.toString();
    }

    @Override
    @Async
    public Future<String> createTsvectorIndexAsync(Map<String, Object> request) throws SQLException {
        log.debug("async procedure for index begins");
        String result = createTsvectorIndex(request);
        log.debug("result returned");
        return new AsyncResult<String>(result);
    }

    /**
     *
     * @param columns
     * @return
     */
    private String makeToTsvectorStatement(final List<Map<String, Object>> columns) {
        String tsvect = columns.stream()
                .map(c -> "coalesce(" + (String) c.get(Field.NAME) + ",'')")
                .collect(Collectors.joining(" || ' ' || "));
        return tsvect;

    }

    /**
     * Reads ts vector type value
     *
     * @param request
     * @return string name of the type, or GIN
     */
    private String readTsvectorType(Map<String, Object> request) {
        return request.containsKey(Field.TYPE) ? (String)request.get(Field.TYPE) : "GIN";
    }

    @Override
    public String dropTsvectorIndex(String name) throws SQLException {
        checkForSQLInjectionAttack(name,ProxySQLOperation.DROP);
        StringBuilder sb = new StringBuilder("DROP INDEX ");
        sb.append(name);
        jdbcTemplate.execute(sb.toString());
        return sb.toString();
    }

    @Override
    public List<String> checkTsvectorIndex(String name) throws SQLException {
        checkForSQLInjectionAttack(name, ProxySQLOperation.SELECT);
        StringBuilder sb = new StringBuilder("SELECT (tablename || '.' || indexname) AS location FROM pg_indexes WHERE indexname = '");
        sb.append(name).append("'");
        List result = jdbcTemplate.queryForList(sb.toString());
        return result;
    }

    @Override
    public Map<String,Object> search(final Map<String,Object> request) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String ftsConfig = readFtsConfigurationParam(request);

        Map<String,Object> tableDescr = (Map<String,Object>)request.get(Field.TABLE);

        List<Map<String,Object>> columns = (List<Map<String,Object>>)tableDescr.get(Field.COLUMNS);

        String selectable = columns.stream()
                .filter(c -> ((Boolean) c.get(Field.SELECTABLE)).equals(Boolean.TRUE))
                .map(c -> (String) c.get(Field.NAME))
                .collect(Collectors.joining(", "));

        String tsvect = columns.stream()
                .filter(c -> ((Boolean) c.get(Field.TSV)).equals(Boolean.TRUE))
                .map(c -> "coalesce(" + (String) c.get(Field.NAME) + ",'')")
                .collect(Collectors.joining(" || ' ' || "));

        sb.append(selectable).append(" FROM ")
                .append(tableDescr.get(Field.NAME))
                .append(" WHERE to_tsvector('").append(ftsConfig).append("',").append(tsvect).append(") ")
                .append("@@ ")
                .append("plainto_tsquery('")
                .append(request.get(Field.QUERY))
                .append("')");

        if ( request.containsKey(Field.ORDERBY)) {
            sb.append(" ORDER BY ").append(request.get(Field.ORDERBY));
        }

        if ( request.containsKey(Field.LIMIT) ) {
            sb.append(" LIMIT ").append(request.get(Field.LIMIT));
        }

        if ( request.containsKey(Field.OFFSET) ) {
            sb.append(" OFFSET ").append(request.get(Field.OFFSET));
        }

        //check for sql injection attack
        checkForSQLInjectionAttack(sb.toString(), ProxySQLOperation.SELECT);
        sb.insert(0,"SELECT ");
        log.debug("search SQL statement: {}", sb.toString());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("fts search");
        //executing FTS search
        List result = jdbcTemplate.queryForList(sb.toString());
        stopWatch.stop();
        Map<String,Object> resultMap = new HashMap<String,Object>();
        resultMap.put(Field.STATEMENT, sb.toString());
        resultMap.put(Field.STAT, stopWatch.getTaskInfo()[0].getTimeMillis());
        resultMap.put(Field.RECORDS,result);

        return resultMap;
    }

    /**
     * Helps determine if partial SQL statement has potential injection attack
     * @param sb
     * @throws SQLException
     */
    private void checkForSQLInjectionAttack(final String sb, ProxySQLOperation operation) throws SQLException {
        StringBuilder check = new StringBuilder(SQL_INJECT_CHECK);
        switch (operation) {
            case SELECT: check.append("|(").append(SQL_INJECT_CHECK).append(")|(").append(SQL_INJECT_CHECK_DROP).append(")"); break;
            case CREATE: check.append("|(").append(SQL_INJECT_CHECK_DROP).append(")"); break;
            case DROP: check.append("|(").append(SQL_INJECT_CHECK_CREATE).append(")"); break;
        }
        Pattern p = Pattern.compile(check.toString());
        Matcher m = p.matcher(sb.toUpperCase());
        if ( m.find() ) {
            log.error("possible SQL injection attack with statement {}",sb.toString());
            throw  new SQLException("SQL Injection", "terminating execution");
        }
    }

    /**
     * reads FTS configuration parameter value from the request
     * @param request JSON deserialized into Map
     * @return string name of the configuration or <i>english</i>
     */
    private String readFtsConfigurationParam(Map<String, Object> request) {
        return request.containsKey(Field.CONFIGURATION) ? (String)request.get(Field.CONFIGURATION) : "english";
    }
}
