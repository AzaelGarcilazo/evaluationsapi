package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.FavoriteSpecializationRequest;
import compass.career.evaluationsapi.dto.FavoriteSpecializationResponse;
import compass.career.evaluationsapi.service.FavoriteSpecializationService;
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
@RequestMapping("/api/v1/favorite-specializations")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Favorite Specializations", description = "Endpoints for managing user's favorite specializations")
public class FavoriteSpecializationController {

    private final FavoriteSpecializationService specializationService;

    @PostMapping
    @Operation(
            summary = "Add a specialization to favorites",
            description = "Allows the user to mark a specialization as favorite. Maximum 5 favorites."
    )
    public ResponseEntity<FavoriteSpecializationResponse> addFavoriteSpecialization(
            @RequestParam Integer userId,
            @Valid @RequestBody FavoriteSpecializationRequest request) {
        FavoriteSpecializationResponse response = specializationService.addFavoriteSpecialization(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/favorite-specializations"))
                .body(response);
    }

    @DeleteMapping("/{specializationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove a specialization from favorites",
            description = "Removes a specialization from the user's favorites list (soft delete)."
    )
    public void removeFavoriteSpecialization(
            @RequestParam Integer userId,
            @PathVariable Integer specializationId) {
        specializationService.removeFavoriteSpecialization(userId, specializationId);
    }

    @GetMapping
    @Operation(
            summary = "Get favorite specializations with pagination",
            description = "Retrieves the list of specializations marked as favorites by the user."
    )
    public List<FavoriteSpecializationResponse> getFavoriteSpecializations(
            @RequestParam Integer userId,
            @Parameter(description = "Page number (starts at 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of favorites per page", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        if (page < 0 || pageSize < 0 || (page == 0 && pageSize == 0)) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters: page and pageSize cannot be negative and cannot both be 0.");
        }

        return specializationService.getFavoriteSpecializations(userId, page, pageSize);
    }
}