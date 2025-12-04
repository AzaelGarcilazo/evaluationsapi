package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.CreateTestRequest;
import compass.career.evaluationsapi.dto.TestListResponse;
import compass.career.evaluationsapi.dto.TestResponse;
import compass.career.evaluationsapi.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Tests", description = "Endpoints for test management")
public class TestController {

    private final TestService testService;

    @GetMapping
    @Operation(summary = "Get all tests", description = "Retrieves all available tests in the system")
    public List<TestListResponse> getAllTests() {
        return testService.getAllTests();
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Get test details", description = "Retrieves detailed information about a specific test")
    public TestResponse getTestDetails(@PathVariable Integer testId) {
        return testService.getTestDetails(testId);
    }

    @PostMapping
    @Operation(summary = "Create a new test (Admin)", description = "Creates a new test with questions")
    public ResponseEntity<TestResponse> createTest(@Valid @RequestBody CreateTestRequest request) {
        TestResponse response = testService.createTest(request);
        return ResponseEntity
                .created(URI.create("/api/v1/tests/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{testId}")
    @Operation(summary = "Update a test (Admin)", description = "Updates an existing test")
    public TestResponse updateTest(
            @PathVariable Integer testId,
            @Valid @RequestBody CreateTestRequest request) {
        return testService.updateTest(testId, request);
    }
}