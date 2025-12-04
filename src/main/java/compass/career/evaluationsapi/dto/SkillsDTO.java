package compass.career.evaluationsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillsDTO {
    private Integer userId;
    private Map<String, Integer> skills; // skillName -> proficiencyLevel
}