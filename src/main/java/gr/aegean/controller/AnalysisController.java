package gr.aegean.controller;

import gr.aegean.model.analysis.AnalysisReport;
import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<List<AnalysisReport>> analyze(@Valid @RequestBody AnalysisRequest request) {
        List<AnalysisReport> reports = analysisService.analyzeProject(request);

        return new ResponseEntity<>(reports, HttpStatus.OK);
    }
}
