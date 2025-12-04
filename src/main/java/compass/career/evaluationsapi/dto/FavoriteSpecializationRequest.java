package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteSpecializationRequest {
    @NotNull
    private Integer specializationAreaId;

    @Size(max = 1000)
    private String notes;    
}