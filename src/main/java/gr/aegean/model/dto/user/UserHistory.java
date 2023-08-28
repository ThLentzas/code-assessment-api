package gr.aegean.model.dto.user;

import gr.aegean.model.dto.analysis.AnalysisResponse;

import java.util.List;

public record UserHistory(List<AnalysisResponse> history) {
}
