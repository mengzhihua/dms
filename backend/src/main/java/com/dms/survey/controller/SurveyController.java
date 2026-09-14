package com.dms.survey.controller;

import com.dms.common.R;
import com.dms.survey.entity.Complaint;
import com.dms.survey.entity.Survey;
import com.dms.survey.service.SurveyService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService service;

    @PostMapping("/{id}/answer")
    public R<Survey> answer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.answer(id, (List<Map<String, Object>>) body.get("answers")));
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats(
            @RequestParam(required = false) String dealerCode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return R.ok(service.stats(dealerCode, from, to));
    }

    @PostMapping("/complaint/{id}/handle")
    public R<Complaint> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(
                service.handleComplaint(
                        id,
                        (String) body.get("status"),
                        (String) body.get("handler"),
                        (String) body.get("resolution")));
    }
}
