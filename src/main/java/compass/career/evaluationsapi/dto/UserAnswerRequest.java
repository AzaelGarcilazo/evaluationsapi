package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class UserAnswerRequest {
    @NotNull
    @JsonProperty("Question Id")
    private Integer questionId;

    @NotNull
    @JsonProperty("Option Id")
    private Integer optionId;
}