package gr.aegean.controller;

import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    /*
         Have a message saying that if in the analysis report they don't see a repository from those they
         provided, it wasn't a valid GitHub repository URL, or it was a private one, or the language was not
         supported.
     */
    @PostMapping("/analysis")
    public ResponseEntity<Void> processProject(@Valid @RequestBody AnalysisRequest request,
                                               HttpServletRequest httpServletRequest,
                                               UriComponentsBuilder uriBuilder) {
        Integer analysisId = projectService.processProject(request, httpServletRequest).join();
        URI location = uriBuilder
                .path("/api/v1/projects/analysis/{analysisId}")
                .buildAndExpand(analysisId)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    //projects/id/analysis/id gia sugkekrimeno project
    //projects/analysis/id gia to history
}
