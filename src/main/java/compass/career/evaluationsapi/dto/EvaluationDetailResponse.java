package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDetailResponse {

    @JsonProperty("Evaluation id")
    private Integer evaluationId;

    @JsonProperty("Test title")
    private String testName;

    @JsonProperty("Test category")
    private String testType;

    @JsonProperty("Completed at")
    private LocalDateTime completionDate;

    @JsonProperty("Overall score")
    private String totalScore;

    @JsonProperty("User responses")
    private List<UserAnswerDetail> answers;

    @JsonProperty("Detailed results")
    private Object analysisResult;
}