package gr.aegean.model.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record AnalysisRequest(
        @NotNull
        @Size(min = 1, message = "Provide at least one GitHub url repository")
        List<String> projectUrls,
        List<Constraint> constraints,
        List<Preference> preferences
) {
}
