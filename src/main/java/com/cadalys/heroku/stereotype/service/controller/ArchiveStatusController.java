package com.cadalys.heroku.stereotype.service.controller;

import com.cadalys.heroku.stereotype.service.ArchiveStatusService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class ArchiveStatusController {

    @Autowired
    private ArchiveStatusService archiveStatusService;

    @Autowired
    private Gson gson;

    @RequestMapping(method = RequestMethod.POST, value = "/archive/status",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody String getByIds(@RequestBody String body) {
        final String[] identifiers = gson.fromJson(body, String[].class);

        final List<Map<String, Object>> records;
        if ((identifiers != null) && (identifiers.length > 0)) {
            records = archiveStatusService.findByIds(identifiers);
        } else {
            records = Collections.emptyList();
        }

        return gson.toJson(records);
    }
}
