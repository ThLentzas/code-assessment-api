package gr.aegean.model.analysis;

import lombok.Getter;


@Getter
public enum QualityMetricOperator {
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    EQ("=="),
    NEQ("<>");

    private final String symbol;

    QualityMetricOperator(String symbol) {
        this.symbol = symbol;
    }
}
