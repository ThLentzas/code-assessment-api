package gr.aegean.model.dto.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;


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
