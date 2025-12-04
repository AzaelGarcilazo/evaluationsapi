package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
public class SubmitTestRequest {
    @NotNull
    @JsonProperty("Test Id")
    private Integer testId;

    @NotEmpty
    @JsonProperty("User Answers")
    private List<UserAnswerRequest> answers;
}