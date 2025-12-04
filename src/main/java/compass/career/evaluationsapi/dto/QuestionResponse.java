package compass.career.evaluationsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QuestionResponse {
    @JsonProperty("Id")
    Integer id;

    @JsonProperty("Text")
    String questionText;

    @JsonProperty("Order")
    Integer orderNumber;

    @JsonProperty("Options")
    List<AnswerOptionResponse> options;
}