package com.xiaohongshu.writer.controller;

import com.xiaohongshu.writer.dto.GenerateRequest;
import com.xiaohongshu.writer.dto.GenerateResponse;
import com.xiaohongshu.writer.service.GenerateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GenerateController {

    private final GenerateService generateService;

    public GenerateController(GenerateService generateService) {
        this.generateService = generateService;
    }

    @PostMapping("/generate")
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest request) {
        return generateService.generate(request);
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
