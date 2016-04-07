package com.cadalys.heroku.exception;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
public abstract class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }
}
