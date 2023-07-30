package gr.aegean.model.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.List;


public record AnalysisRequest(
        // @NotBlank(message = "Provide at least one GitHub url repository")
        List<String> projectUrls,
        List<Constraint> constraints,
        List<Preference> preferences
) {
}
