package compass.career.evaluationsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoDTO {
    private Integer userId;
    private String name;
    private String email;
    private String role;
}