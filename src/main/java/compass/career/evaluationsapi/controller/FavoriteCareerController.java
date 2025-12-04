package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.FavoriteCareerRequest;
import compass.career.evaluationsapi.dto.FavoriteCareerResponse;
import compass.career.evaluationsapi.service.FavoriteCareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/favorite-careers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Favorite Careers", description = "Endpoints for managing user's favorite careers")
public class FavoriteCareerController {

    private final FavoriteCareerService careerService;

    @PostMapping
    @Operation(
            summary = "Add a career to favorites",
            description = "Allows the user to mark a career as favorite. Maximum 10 favorite careers."
    )
    public ResponseEntity<FavoriteCareerResponse> addFavoriteCareer(
            @RequestParam Integer userId,
            @Valid @RequestBody FavoriteCareerRequest request) {
        FavoriteCareerResponse response = careerService.addFavoriteCareer(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/favorite-careers"))
                .body(response);
    }

    @DeleteMapping("/{careerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove a career from favorites",
            description = "Removes a career from the user's favorites list (soft delete)."
    )
    public void removeFavoriteCareer(
            @RequestParam Integer userId,
            @PathVariable Integer careerId) {
        careerService.removeFavoriteCareer(userId, careerId);
    }

    @GetMapping
    @Operation(
            summary = "Get favorite careers with pagination",
            description = "Retrieves the list of careers marked as favorites by the user."
    )
    public List<FavoriteCareerResponse> getFavoriteCareers(
            @RequestParam Integer userId,
            @Parameter(description = "Page number (starts at 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of favorite careers per page", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        if (page < 0 || pageSize < 0 || (page == 0 && pageSize == 0)) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters: page and pageSize cannot be negative and cannot both be 0.");
        }

        return careerService.getFavoriteCareers(userId, page, pageSize);
    }
}