package gr.aegean.controller;

import gr.aegean.model.entity.AnalysisReport;
import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping("/analysis")
    public ResponseEntity<List<AnalysisReport>> processProject(@Valid @RequestBody AnalysisRequest request) {
        List<AnalysisReport> reports = projectService.processProject(request).join();

        return new ResponseEntity<>(reports, HttpStatus.OK);
    }
}
