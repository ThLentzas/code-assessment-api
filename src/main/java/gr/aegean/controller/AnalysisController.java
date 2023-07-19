package gr.aegean.controller;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.service.AnalysisService;
import gr.aegean.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analysis")
public class AnalysisController {
    private final ProjectService projectService;
    private final AnalysisService analysisService;

    /*
         Have a message saying that if in the analysis report they don't see a repository from those they
         provided, it wasn't a valid GitHub repository URL, or it was a private one, or the language was not
         supported.
     */
    @PostMapping
    public ResponseEntity<Void> analyze(@Valid @RequestBody AnalysisRequest request,
                                               HttpServletRequest httpServletRequest,
                                               UriComponentsBuilder uriBuilder) {
        Integer analysisId = projectService.processProject(request, httpServletRequest).join();
        URI location = uriBuilder
                .path("/api/v1/analysis/{analysisId}")
                .buildAndExpand(analysisId)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    /*
        Returns a list of analysis reports for all the repositories submitted.
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<List<AnalysisReport>> getAnalysisResult(@PathVariable Integer analysisId) {
        List<AnalysisReport> reports = analysisService.findAnalysisReportByAnalysisId(analysisId);

        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    /*
        Returns the analysis report for a specific repository.
     */
    @GetMapping("/report/{reportId}")
    public ResponseEntity<AnalysisReport> getAnalysisReport(@PathVariable Integer reportId) {
        // TODO: 7/20/2023 Add projectUrl property in analysis report.
//        List<AnalysisReport> reports = analysisService.findAnalysisReportById(reportId);
//
//        return new ResponseEntity<>(reports, HttpStatus.OK);
        return null;
    }
}
