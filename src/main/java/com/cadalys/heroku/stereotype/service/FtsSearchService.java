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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Service to provide FTS search, index creation and re-indexing.
 *
 */
public interface FtsSearchService {

    /**
     * Assembles SQL statement to create index and executes the statement
     *
     * @param request
     * @return
     * @throws SQLException
     */
    String createTsvectorIndex(final Map<String,Object> request) throws SQLException;

    /**
     * Executes index create statement asynchronously
     *
     * @param request
     * @return
     * @throws SQLException
     */
    Future<String> createTsvectorIndexAsync(Map<String, Object> request) throws SQLException;

    /**
     * Drops index by name
     *
     * @param name
     * @return
     * @throws SQLException
     */
    String dropTsvectorIndex(final String name) throws SQLException;

    /**
     * Checks if index exists
     *
     * @param name - name of the index to check on
     * @return
     * @throws SQLException
     */
    List<String> checkTsvectorIndex(final String name) throws SQLException;

    /**
     * Assembles a SQL statement for FTS search and runs it to obtain the result records
     *
     * @param request map created as the resutl of JSON message deserialization
     * @return returns result of the search, it time and the SQL statement generated based on the request map
     * @throws SQLException
     */
    Map<String,Object> search(final Map<String,Object> request) throws SQLException;

}
