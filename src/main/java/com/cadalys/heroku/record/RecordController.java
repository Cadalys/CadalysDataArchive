package com.cadalys.heroku.record;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by dzmitrykalachou on 05.04.16.
 */

@RestController
public class RecordController {


    @Autowired
    private RecordService recordService;

    /**
     * Api call to delete archived records
     *
     * @param objects
     * @return
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/record")
    public ResponseEntity<String> deleteRecords(@RequestBody RecordObjects objects) {
        recordService.deleteRecords(objects);
        return new ResponseEntity<>(HttpStatus.OK);
    }



}
