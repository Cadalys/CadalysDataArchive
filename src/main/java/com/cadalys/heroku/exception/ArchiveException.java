package com.cadalys.heroku.exception;

import java.util.List;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
public class ArchiveException extends ApplicationException {


    private final List<String> errors;


    public ArchiveException(List<String> errors) {
        super(null);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
