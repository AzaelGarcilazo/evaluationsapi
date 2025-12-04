package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.service.CareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/careers")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Careers", description = "Endpoints for university career management")
public class CareerController {

    private final CareerService careerService;

    @PostMapping("/recommendations")
    @Operation(
            summary = "Get personalized career recommendations",
            description = "Generates or retrieves career recommendations based on the user's evaluation results. Uses Groq AI."
    )
    public List<CareerRecommendationResponse> getRecommendedCareers(@RequestParam Integer userId) {
        return careerService.getRecommendedCareers(userId);
    }

    @GetMapping("/details/{careerId}")
    @Operation(
            summary = "Get complete career details",
            description = "Retrieves detailed information about a specific career including social media data."
    )
    public CareerDetailResponse getCareerDetails(@PathVariable Integer careerId) {
        return careerService.getCareerDetails(careerId);
    }

    @GetMapping
    @Operation(
            summary = "List all available careers with pagination",
            description = "Retrieves the catalog of university careers with pagination support."
    )
    public List<CareerResponse> getAllCareers(
            @Parameter(description = "Page number (starts at 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of careers per page", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        return careerService.getAllCareers(page, pageSize);
    }

    @GetMapping("/{careerId}")
    @Operation(
            summary = "Get basic career information by ID",
            description = "Retrieves the basic data of a specific career."
    )
    public CareerResponse getCareerById(@PathVariable Integer careerId) {
        return careerService.getCareerById(careerId);
    }

    @PostMapping
    @Operation(
            summary = "Create a new career (Admin)",
            description = "Registers a new career in the system."
    )
    public ResponseEntity<CareerResponse> createCareer(@Valid @RequestBody CareerRequest request) {
        CareerResponse response = careerService.createCareer(request);
        return ResponseEntity
                .created(URI.create("/api/v1/careers/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{careerId}")
    @Operation(
            summary = "Update an existing career (Admin)",
            description = "Modifies the information of a career."
    )
    public CareerResponse updateCareer(
            @PathVariable Integer careerId,
            @Valid @RequestBody CareerRequest request) {
        return careerService.updateCareer(careerId, request);
    }
}