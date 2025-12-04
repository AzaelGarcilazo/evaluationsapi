package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.service.SpecializationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Specializations", description = "Endpoints for academic specialization management")
public class SpecializationController {

    private final SpecializationService specializationService;

    @PostMapping("/recommendations")
    @Operation(
            summary = "Get personalized specialization recommendations",
            description = "Generates or retrieves specialization recommendations based on user's profile and skills."
    )
    public List<SpecializationRecommendationResponse> getRecommendedSpecializations(@RequestParam Integer userId) {
        return specializationService.getRecommendedSpecializations(userId);
    }

    @GetMapping("/details/{specializationId}")
    @Operation(
            summary = "Get complete specialization details",
            description = "Retrieves detailed information about a specific specialization."
    )
    public SpecializationDetailResponse getSpecializationDetails(@PathVariable Integer specializationId) {
        return specializationService.getSpecializationDetails(specializationId);
    }

    @GetMapping
    @Operation(
            summary = "List all available specializations",
            description = "Retrieves the catalog of academic specializations."
    )
    public List<SpecializationAreaResponse> getAllSpecializations() {
        return specializationService.getAllSpecializations();
    }

    @GetMapping("/{specializationId}")
    @Operation(
            summary = "Get basic specialization information by ID",
            description = "Retrieves the basic data of a specific specialization."
    )
    public SpecializationAreaResponse getSpecializationById(@PathVariable Integer specializationId) {
        return specializationService.getSpecializationById(specializationId);
    }

    @PostMapping
    @Operation(
            summary = "Create a new specialization (Admin)",
            description = "Registers a new specialization in the system."
    )
    public ResponseEntity<SpecializationAreaResponse> createSpecialization(
            @Valid @RequestBody SpecializationAreaRequest request) {
        SpecializationAreaResponse response = specializationService.createSpecialization(request);
        return ResponseEntity
                .created(URI.create("/api/v1/specializations/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{specializationId}")
    @Operation(
            summary = "Update an existing specialization (Admin)",
            description = "Modifies the information of a specialization."
    )
    public SpecializationAreaResponse updateSpecialization(
            @PathVariable Integer specializationId,
            @Valid @RequestBody SpecializationAreaRequest request) {
        return specializationService.updateSpecialization(specializationId, request);
    }
}