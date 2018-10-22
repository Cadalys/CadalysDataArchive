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
package com.cadalys.heroku.config;

import com.cadalys.heroku.exception.ArchiveException;
import com.cadalys.heroku.exception.ArchiveStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.cadalys.heroku.exception.AuthException;
import com.cadalys.heroku.exception.ErrorsResponseBean;

@ControllerAdvice
public class ErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ArchiveException.class)
    public ErrorsResponseBean handleArchiveException(ArchiveException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ArchiveStatusException.class)
    public ErrorsResponseBean handleArchiveStatusException(ArchiveStatusException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorsResponseBean handleApplicationException(Exception e) {
        return handleException(e);
    }


    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorsResponseBean processInvalidTokenException(AuthException ex) {
        return handleApplicationException(ex);
    }

    private ErrorsResponseBean handleException(Exception e) {
        logger.warn(e.getMessage(), e);
        if (e instanceof ArchiveException) {
            return new ErrorsResponseBean(((ArchiveException) e).getErrors());
        } else if (e instanceof ArchiveStatusException) {
            return new ErrorsResponseBean(((ArchiveStatusException) e).getErrors());
        } else {
            String message = e.getMessage();
            if (e.getLocalizedMessage() != null) {
                message = e.getLocalizedMessage();
            }
            return new ErrorsResponseBean(message);
        }
    }


}
