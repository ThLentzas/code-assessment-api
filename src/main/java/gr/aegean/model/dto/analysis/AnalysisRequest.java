package gr.aegean.model.dto.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.List;


/*
    The @Valid annotation in the property will validate the Constraint object inside the constraints array, meaning if
    we pass constraints[] (empty array) is fine and acceptable by the app, user simply didn't provide any constraints,
    but if we pass constraints[{}] (an array with an empty object) validation will be performed in that object and the
    relevant error message will be thrown. Similar for preferences.
 */
public record AnalysisRequest(

        @NotNull(message = "Provide at least one GitHub url repository")
        @Size(min = 1, message = "Provide at least one GitHub url repository")
        List<String> projectUrls,
        @Valid
        List<Constraint> constraints,
        @Valid
        List<Preference> preferences) {
}
