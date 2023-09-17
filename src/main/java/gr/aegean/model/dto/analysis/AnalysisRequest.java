package gr.aegean.model.dto.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.List;


public record AnalysisRequest(
        @NotNull(message = "Provide at least one GitHub url repository")
        @Size(min = 1, message = "Provide at least one GitHub url repository")
        List<String> projectUrls,
        @Valid
        List<Constraint> constraints,
        @Valid
        List<Preference> preferences) {
}
