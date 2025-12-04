package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@Value
@Builder
public class CareerDetailResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Duration Semesters")
    Integer durationSemesters;

    @JsonProperty("Graduate Profile")
    String graduateProfile;

    @JsonProperty("Job Field")
    String jobField;

    @JsonProperty("Average Salary")
    BigDecimal averageSalary;

    @JsonProperty("Social Media Data")
    Object socialMediaData;
}