package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    @NotBlank
    private String questionText;

    @NotNull
    private Integer orderNumber;

    @NotEmpty
    private List<AnswerOptionRequest> options;
}