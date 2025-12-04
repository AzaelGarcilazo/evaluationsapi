package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerDetail {

    @JsonProperty("Question id")
    private Integer questionId;

    @JsonProperty("Question")
    private String questionText;

    @JsonProperty("Selected option id")
    private Integer selectedOptionId;

    @JsonProperty("Selected answer")
    private String selectedOptionText;

    @JsonProperty("Area category")
    private String category;

    @JsonProperty("Points earned")
    private Integer weightValue;
}