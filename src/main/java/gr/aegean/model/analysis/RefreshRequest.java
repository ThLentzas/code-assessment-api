package gr.aegean.model.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.ArrayList;
import java.util.List;

public record RefreshRequest(List<Constraint> constraints, List<Preference> preferences) {
    public RefreshRequest {
        if (constraints == null) {
            constraints = new ArrayList<>();
        }

        if (preferences == null) {
            preferences = new ArrayList<>();
        }
    }
}
