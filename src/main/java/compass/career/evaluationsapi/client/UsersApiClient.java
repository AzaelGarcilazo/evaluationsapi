package compass.career.evaluationsapi.client;

import compass.career.evaluationsapi.dto.SkillsDTO;
import compass.career.evaluationsapi.dto.UserBasicInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsersApiClient {

    private final RestTemplate restTemplate;

    @Value("${usersapi.url}")
    private String usersApiUrl;

    public UserBasicInfoDTO getUserBasicInfo(Integer userId) {
        try {
            String url = usersApiUrl + "/api/v1/users/" + userId + "/basic-info";
            log.info("Calling UsersAPI: {}", url);

            ResponseEntity<UserBasicInfoDTO> response = restTemplate.getForEntity(
                    url,
                    UserBasicInfoDTO.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("User not found in UsersAPI: {}", userId);
                throw new IllegalArgumentException("User not found");
            }
            log.error("Error calling UsersAPI for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error communicating with UsersAPI", e);
        }
    }

    public SkillsDTO getUserSkills(Integer userId) {
        try {
            String url = usersApiUrl + "/api/v1/users/" + userId + "/skills";
            log.info("Calling UsersAPI for skills: {}", url);

            ResponseEntity<SkillsDTO> response = restTemplate.getForEntity(
                    url,
                    SkillsDTO.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("User not found in UsersAPI: {}", userId);
                throw new IllegalArgumentException("User not found");
            }
            log.error("Error calling UsersAPI for user skills {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error communicating with UsersAPI", e);
        }
    }

    public boolean userExists(Integer userId) {
        try {
            String url = usersApiUrl + "/api/v1/users/" + userId + "/exists";
            log.info("Checking if user exists in UsersAPI: {}", url);

            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    url,
                    Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error checking if user exists: {}", userId, e);
            return false;
        }
    }
}