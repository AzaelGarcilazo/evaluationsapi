package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SpecializationAreaResponse {
    Integer id;
    String name;
    String description;
    String applicationFields;
    String jobProjection;
    Integer careerId;
    String careerName;
}