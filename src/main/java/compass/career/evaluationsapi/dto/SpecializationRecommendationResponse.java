package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class SpecializationRecommendationResponse {
    Integer id;
    String name;
    String description;
    BigDecimal compatibilityPercentage;
    String careerName;
}