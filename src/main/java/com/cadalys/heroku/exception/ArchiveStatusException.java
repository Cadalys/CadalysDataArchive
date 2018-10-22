package com.cadalys.heroku.exception;

import java.util.List;

public class ArchiveStatusException extends ApplicationException {

    private List<String> errors;

    public ArchiveStatusException(final List<String> errors) {
        super(null);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
