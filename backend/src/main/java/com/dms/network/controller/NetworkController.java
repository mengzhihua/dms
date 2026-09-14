package com.dms.network.controller;

import com.dms.common.R;
import com.dms.network.entity.DealerAssessment;
import com.dms.network.service.NetworkService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {
    private final NetworkService service;

    @GetMapping("/target/achievement")
    public R<Map<String, Object>> achievement(
            @RequestParam String dealerCode, @RequestParam String yearMonth) {
        return R.ok(service.achievement(dealerCode, yearMonth));
    }

    @PostMapping("/assessment/generate")
    public R<DealerAssessment> generate(
            @RequestParam String dealerCode, @RequestParam String yearMonth) {
        return R.ok(service.generateAssessment(dealerCode, yearMonth));
    }
}
