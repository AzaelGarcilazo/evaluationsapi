package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@Value
@Builder
public class CareerRecommendationResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Compatibility Percentage")
    BigDecimal compatibilityPercentage;

    @JsonProperty("Duration Semesters")
    Integer durationSemesters;

    @JsonProperty("Average Salary")
    BigDecimal averageSalary;
}