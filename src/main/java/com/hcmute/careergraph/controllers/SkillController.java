package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.services.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@Slf4j
@RequestMapping("skills")
@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping("lookup")
    public RestResponse<List<HashMap<String, String>>> lookup(@RequestParam(required = false)String query) {
        return RestResponse.<List<HashMap<String,String>>>builder()
                .data(skillService.getLookupSkill(query))
                .status(HttpStatus.OK)
                .build();
    }
}
