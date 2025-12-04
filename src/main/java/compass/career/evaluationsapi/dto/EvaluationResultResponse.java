package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class EvaluationResultResponse {
    @JsonProperty("Id")
    Integer evaluationId;

    @JsonProperty("Name")
    String testName;

    @JsonProperty("Type")
    String testType;

    @JsonProperty("Completed at")
    LocalDateTime completionDate;

    @JsonProperty("Score")
    BigDecimal totalScore;

    @JsonProperty("Details")
    Object resultDetails;
}