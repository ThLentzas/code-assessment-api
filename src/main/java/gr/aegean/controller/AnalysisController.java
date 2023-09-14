package gr.aegean.controller;

import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.analysis.AsyncService;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.model.dto.analysis.AnalysisResponse;
import gr.aegean.model.dto.analysis.RefreshRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
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
                                        HttpServletRequest httpServletRequest,
                                        UriComponentsBuilder uriBuilder) {
        Integer analysisId = asyncService.processProject(analysisRequest, httpServletRequest).join();
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
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Integer analysisId,
                                               HttpServletRequest request) {
        analysisService.deleteAnalysis(analysisId, request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{analysisId}/request")
    public ResponseEntity<AnalysisRequest> getAnalysisRequest(@PathVariable Integer analysisId) {
       AnalysisRequest request = analysisService.findAnalysisRequestByAnalysisId(analysisId);

       return new ResponseEntity<>(request, HttpStatus.OK);
    }
}
