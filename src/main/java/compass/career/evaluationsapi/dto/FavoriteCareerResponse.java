package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;
import com.fasterxml.jackson.annotation.JsonProperty;

@Value
@Builder
public class FavoriteCareerResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Career Id")
    Integer careerId;

    @JsonProperty("Career Name")
    String careerName;

    @JsonProperty("Notes")
    String notes;

    @JsonProperty("Active")
    Boolean active;
}