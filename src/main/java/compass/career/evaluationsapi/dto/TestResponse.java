package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TestResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Type")
    String testType;

    @JsonProperty("Questions to show")
    Integer questionsToShow;

    @JsonProperty("Questions")
    List<QuestionResponse> questions;
}