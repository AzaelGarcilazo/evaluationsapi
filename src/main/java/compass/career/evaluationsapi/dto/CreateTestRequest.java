package compass.career.evaluationsapi.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTestRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private Integer testTypeId;

    @NotNull
    @Min(1)
    private Integer questionsToShow;

    @NotNull
    @Size(min = 100)
    private List<QuestionRequest> questions;
}