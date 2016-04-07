package com.cadalys.heroku.exception;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
public class AuthException extends ApplicationException {

    public AuthException() {
        super("Invalid basic auth data");
    }
}
