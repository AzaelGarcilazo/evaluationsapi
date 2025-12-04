package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnswerOptionResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Text")
    String optionText;
}