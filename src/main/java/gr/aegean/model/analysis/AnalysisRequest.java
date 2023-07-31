package gr.aegean.model.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.List;


// TODO: 8/1/2023 validation for everything to not exceed 1.0
public record AnalysisRequest(
        // @NotBlank(message = "Provide at least one GitHub url repository")
        List<String> projectUrls,
        List<Constraint> constraints,
        List<Preference> preferences
) {
}
