package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CareerResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Duration semesters")
    Integer durationSemesters;

    @JsonProperty("Graduate profile")
    String graduateProfile;

    @JsonProperty("Job field")
    String jobField;

    @JsonProperty("Average salary")
    BigDecimal averageSalary;
}