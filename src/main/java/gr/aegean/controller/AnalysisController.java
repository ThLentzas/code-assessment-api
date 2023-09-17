package gr.aegean.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.analysis.AsyncService;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.model.dto.analysis.AnalysisResponse;
import gr.aegean.model.dto.analysis.RefreshRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.net.URI;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analysis")
public class AnalysisController {
    private final AsyncService asyncService;
    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<Void> analyze(@Valid @RequestBody AnalysisRequest analysisRequest,
                                        UriComponentsBuilder uriBuilder) {
        Integer analysisId = asyncService.processProject(analysisRequest).join();
        URI location = uriBuilder
                .path("/api/v1/analysis/{analysisId}")
                .buildAndExpand(analysisId)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisResponse> getAnalysisResult(@PathVariable Integer analysisId) {
        AnalysisResponse result = analysisService.findAnalysisResultByAnalysisId(analysisId);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/{analysisId}")
    public ResponseEntity<AnalysisResponse> refreshAnalysisResult(@RequestBody RefreshRequest refreshRequest,
                                                                  @PathVariable Integer analysisId) {
        AnalysisResponse result = analysisService.refreshAnalysisResult(analysisId, refreshRequest);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Integer analysisId) {
        analysisService.deleteAnalysis(analysisId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{analysisId}/request")
    public ResponseEntity<AnalysisRequest> getAnalysisRequest(@PathVariable Integer analysisId) {
        AnalysisRequest request = analysisService.findAnalysisRequestByAnalysisId(analysisId);

        return new ResponseEntity<>(request, HttpStatus.OK);
    }
}
