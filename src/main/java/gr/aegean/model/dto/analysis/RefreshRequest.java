package gr.aegean.model.dto.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

/*
    The @Valid annotation in the property will validate the Constraint object inside the constraints array, meaning if
    we pass constraints[] (empty array) is fine and acceptable by the app, user simply didn't provide any constraints,
    but if we pass constraints[{}] (an array with an empty object) validation will be performed in that object and the
    relevant error message will be thrown. Similar for preferences.
 */
public record RefreshRequest(@Valid List<Constraint> constraints,
                             @Valid List<Preference> preferences) {

    public RefreshRequest {
        if (constraints == null) {
            constraints = new ArrayList<>();
        }

        if (preferences == null) {
            preferences = new ArrayList<>();
        }
    }
}
