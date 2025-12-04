package compass.career.evaluationsapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TestListResponse {
    Integer id;
    String name;
    String testType;
    Integer questionsCount;
    Boolean active;    
}