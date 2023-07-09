package gr.aegean.model.analysis;

import jakarta.validation.constraints.NotBlank;

import java.util.List;


public record AnalysisRequest(
       // @NotBlank(message = "Provide at least one GitHub url repository")
        List<String> projectUrls) {}
//private qualityAttribues
