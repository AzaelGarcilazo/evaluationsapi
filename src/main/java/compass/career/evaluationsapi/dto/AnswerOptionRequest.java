package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnswerOptionRequest {
    @NotBlank
    private String optionText;

    private Integer weightValue;

    private String category; 
}