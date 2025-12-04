package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpecializationAreaRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @Size(max = 2000)
    private String applicationFields;

    @Size(max = 2000)
    private String jobProjection;

    @NotNull
    private Integer careerId;
}