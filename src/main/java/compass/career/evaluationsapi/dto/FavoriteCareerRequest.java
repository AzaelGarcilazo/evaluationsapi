package compass.career.evaluationsapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class FavoriteCareerRequest {
    @NotNull
    @JsonProperty("Career Id")
    private Integer careerId;

    @Size(max = 1000)
    @JsonProperty("Notes")
    private String notes;
}