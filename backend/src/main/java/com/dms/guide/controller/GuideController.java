package com.dms.guide.controller;

import com.dms.common.R;
import com.dms.guide.service.GuideService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController {
    private final GuideService service;

    @PostMapping("/recommend")
    public R<List<Map<String, Object>>> recommend(@RequestBody Map<String, Object> body) {
        return R.ok(
                service.recommend(
                        (String) body.get("modelCode"),
                        (List<String>) body.get("dtcCodes"),
                        (String) body.get("symptom"),
                        (String) body.get("dealerCode")));
    }

    @GetMapping("/estimate")
    public R<Map<String, Object>> estimate(
            @RequestParam String guideCode, @RequestParam(required = false) String dealerCode) {
        return R.ok(service.estimate(guideCode, dealerCode));
    }
}
