package com.cadalys.heroku.config;

import com.cadalys.heroku.exception.ArchiveException;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.cadalys.heroku.exception.AuthException;
import com.cadalys.heroku.exception.ErrorsResponseBean;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */

@ControllerAdvice
public class ErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class);


    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ArchiveException.class)
    public ErrorsResponseBean handleArchiveException(ArchiveException e) {
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
        LOGGER.warn(e.getMessage(), e);
        if (e instanceof ArchiveException) {
            return new ErrorsResponseBean(((ArchiveException) e).getErrors());
        } else {
            String message = e.getMessage();
            if (e.getLocalizedMessage() != null) {
                message = e.getLocalizedMessage();
            }
            return new ErrorsResponseBean(message);
        }
    }


}
