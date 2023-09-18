package gr.aegean.model.dto.user;

import gr.aegean.model.dto.analysis.AnalysisResult;

import java.util.List;


public record UserHistory(List<AnalysisResult> analyses) {
}
