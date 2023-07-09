package gr.aegean.controller;

import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping
    public void analyze(@Valid @RequestBody AnalysisRequest request) {
        analysisService.analyzeProject(request);
    }
}
