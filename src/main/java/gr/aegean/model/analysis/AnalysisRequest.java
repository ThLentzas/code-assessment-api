package gr.aegean.model.analysis;

import gr.aegean.entity.QualityMetricDetails;

import java.util.List;


public record AnalysisRequest(
        // @NotBlank(message = "Provide at least one GitHub url repository")
       List<String> projectUrls,
       //@NotBlank
       List<QualityMetricDetails> qualityMetricDetails
)
{}
//private qualityAttribues
