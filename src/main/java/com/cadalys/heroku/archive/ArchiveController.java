package com.cadalys.heroku.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by dzmitrykalachou on 19.12.15.
 */
@RestController
public class ArchiveController {


    @Autowired
    private ArchiveService archiveService;

    /**
     * Api call to archive specified objects
     *
     * @param objects
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/archive")
    public ResponseEntity<String> archiveObjects(@RequestBody ArchiveObjects objects) {
        archiveService.checkObjects(objects);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Api call to delete triggers of specified objects
     *
     * @param objects
     * @return
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/archive")
    public ResponseEntity<String> deleteTrigger(@RequestBody ArchiveTableNames objects) {
        archiveService.deleteObjectsTrigger(objects);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
