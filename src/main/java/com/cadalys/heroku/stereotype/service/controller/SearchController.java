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
package com.cadalys.heroku.stereotype.service.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.cadalys.heroku.Field;
import com.cadalys.heroku.stereotype.service.FtsSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for all REST services exposed by the application.
 *
 */
@Controller
public class SearchController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Gson gson;

    @Autowired
    private FtsSearchService ftsSearchService;

    /**
     * Service to be used as a proxy to retrieve results of the FTS search on a single table
     * within the configured Postgres database.
     *
     * @param body - message to be used to create FTS search statement. <br/>
     *             Body format sample:<br/>
     * <pre>
     *  {@code {
     *  "table":{
     *    "name":"salesforce.a__account",
     *    "columns":[
     *      {"name":"name","selectable":true,"tsinclude":true},
     *      {"name":"accountnumber","selectable":true,"tsinclude":false}
     *    ]},
     *    "query":"GenePoint",
     *    "configuration":"english"
     *}
     *  }
     * </pre>
     * @return - result of the FTS search or error. <br/>
     * Sample of the successfull response:
     * <pre>
     * {@code {
     *    "result":{
     *      "sqlstatement":"SELECT id, product_name FROM products WHERE to_tsvector(\u0027english\u0027,coalesce(description,\u0027\u0027) || \u0027 \u0027 || co
     *      alesce(product_name,\u0027\u0027)) @@ plainto_tsquery(\u0027need to connect\u0027)",
     *      "records":[{  "id":33,"product_name":"Genesys Connect for Service Cloud" }],
     *      "javatimemls":1560
     *    },
     *    "error":false
     *}
     * }
     * </pre>
     *
     * Sample of the failed request
     * <pre>
     * {@code {
     *      "error_message":"StatementCallback; bad SQL grammar [SELECT idb, product_name FROM products WHERE to_tsvec.....)]; nested exception is org.postgresql.util.PSQLException: ERROR: column \"idb\" does not exist\n  Position: 8",
     *      "error":true
     *}
     * }
     * </pre>
     */
    @RequestMapping(value = "search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String search(@RequestBody String body) {
        Map<String,Object> searchRequest = gson.fromJson(body, Map.class);
        Map<String,Object> responseMap = new HashMap<String,Object>();
        try {
            Map<String,Object> searchResult = ftsSearchService.search(searchRequest);
            responseMap.put(Field.RESULT, searchResult);
            responseMap.put(Field.ERROR,false);
        } catch (Exception e) {
            log.error("search error: {}",e.getMessage());
            responseMap.put(Field.ERROR,true);
            responseMap.put(Field.ERROR_MSG,e.getMessage());
        }
        //null values in searchResult map now should not be ignored
        return new GsonBuilder().serializeNulls().create().toJson(responseMap);
    }


    /**
     * Service to create ts_vector index
     *
     * @param body JSON string with the instructions for index creation
     *             <pre>
     *             {"name":"idx_prddescr",
     *             "table":{
     *                     "name":"products",
     *                     "columns":[
     *                              {"name":"description"},
     *                              {"name":"product_name"}
     *                              ]}}
     *             </pre>
     * @return Status for current operation
     */
    @RequestMapping(value = "search/index", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String createIndex(@RequestBody String body) {
        Map<String,Object> request = gson.fromJson(body, Map.class);
        Map<String,Object> responseMap = new HashMap<>(0);
        try {
            String resp = ftsSearchService.createTsvectorIndex(request);
            responseMap.put(Field.RESULT, "created");
            responseMap.put(Field.STATEMENT, resp);
            responseMap.put(Field.ERROR,false);
        } catch (Exception e) {
            log.error("failed index creation {}",e);
            log.error("index create error: {}",e.getMessage());
            responseMap.put(Field.ERROR,true);
            responseMap.put(Field.ERROR_MSG,e.getMessage());
        }
        return gson.toJson(responseMap);
    }

    /**
     * Same as createIndex but the logic is run in async mode, hence user will have to check if index had been created via GET method to index endpoint
     * @param body
     * @return
     */
    @RequestMapping(value = "search/async/index", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String createIndexAsync(@RequestBody String body) {
        Map<String,Object> request = gson.fromJson(body, Map.class);
        Map<String,Object> responseMap = new HashMap<>(0);
        try {
            log.debug("starting index creation process in async mode");
            ftsSearchService.createTsvectorIndexAsync(request);
            responseMap.put(Field.RESULT, "scheduled");
            responseMap.put(Field.MESSAGE, "check later for index create status");
            responseMap.put(Field.ERROR,false);
        } catch (Exception e) {
            log.error("failed index creation {}",e);
            log.error("index create error: {}",e.getMessage());
            responseMap.put(Field.ERROR,true);
            responseMap.put(Field.ERROR_MSG,e.getMessage());
        }
        return gson.toJson(responseMap);
    }

    /**
     * Service to drop an index
     * @param body - JSON string with the name of index specified
     *             <pre>
     *             {"name":"idx_prddescr"}
     *             </pre>
     * @return JSON string with the status of current operation
     */
    @RequestMapping(value = "search/index", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String dropIndex(@RequestBody String body) {
        Map<String,Object> request = gson.fromJson(body, Map.class);
        Map<String,Object> responseMap = new HashMap<>(0);
        try {
            String resp = ftsSearchService.dropTsvectorIndex((String) request.get(Field.NAME));
            responseMap.put(Field.RESULT, "dropped");
            responseMap.put(Field.STATEMENT, resp);
            responseMap.put(Field.ERROR,false);
        } catch (Exception e) {
            log.error("failed index delete {}",e);
            log.error("index delete error: {}",e.getMessage());
            responseMap.put(Field.ERROR,true);
            responseMap.put(Field.ERROR_MSG,e.getMessage());
        }

        return gson.toJson(responseMap);
    }


    /**
     * Service to drop an index
     * @param body - JSON string with the name of index specified
     *             <pre>
     *             {"name":"idx_prddescr"}
     *             </pre>
     * @return JSON string with the status of current operation
     */
    @RequestMapping(value = "search/index", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String checkIndex(@RequestBody String body) {
        Map<String,Object> request = gson.fromJson(body, Map.class);
        Map<String,Object> responseMap = new HashMap<>(0);
        try {
            List resp = ftsSearchService.checkTsvectorIndex((String) request.get(Field.NAME));
            if ( resp == null || resp.isEmpty() ) throw new Exception("index doen't exist");
            responseMap.put(Field.RESULT, resp);
            responseMap.put(Field.ERROR,false);
        } catch (Exception e) {
            log.error("index check error: {}",e.getMessage());
            responseMap.put(Field.ERROR,true);
            responseMap.put(Field.ERROR_MSG,e.getMessage());
        }

        return gson.toJson(responseMap);
    }
}
