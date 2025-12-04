package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SpecializationDetailResponse {
    Integer id;
    String name;
    String description;
    String applicationFields;
    String jobProjection;
    String careerName;
    Object socialMediaData;
}