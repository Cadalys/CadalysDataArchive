package com.cadalys.heroku.exception;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
public class ErrorsResponseBean implements Serializable {

    private final List<String> errors;

    public ErrorsResponseBean(String error) {
        errors = Arrays.asList(error);
    }

    public ErrorsResponseBean(List<String> errors) {
        this.errors = errors;
    }


    public List<String> getErrors() {
        return errors;
    }

}
