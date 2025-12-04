package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class EvaluationHistoryResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Name")
    String testName;

    @JsonProperty("Type")
    String testType;

    @JsonProperty("Completed at")
    LocalDateTime completionDate;

    @JsonProperty("Score")
    BigDecimal totalScore;
}