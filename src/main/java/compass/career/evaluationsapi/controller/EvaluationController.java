package compass.career.evaluationsapi.controller;

import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Tag(name = "Evaluations", description = "Endpoints for vocational assessments and psychometric tests")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/personality-test")
    @Operation(
            summary = "Get personality test",
            description = "Retrieves the questions for the personality test based on the Holland RIASEC model."
    )
    public TestResponse getPersonalityTest() {
        return evaluationService.getPersonalityTest();
    }

    @PostMapping("/personality-test")
    @Operation(
            summary = "Submit personality test answers",
            description = "Processes the user's responses to the personality test and generates an analysis using Azure Cognitive Services."
    )
    public ResponseEntity<EvaluationResultResponse> submitPersonalityTest(
            @RequestParam Integer userId,
            @Valid @RequestBody SubmitTestRequest request) {
        EvaluationResultResponse response = evaluationService.submitPersonalityTest(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/evaluations/history"))
                .body(response);
    }

    @GetMapping("/vocational-interests-test")
    @Operation(
            summary = "Get vocational interests test",
            description = "Retrieves the questions for the vocational interests test."
    )
    public TestResponse getVocationalInterestsTest() {
        return evaluationService.getVocationalInterestsTest();
    }

    @PostMapping("/vocational-interests-test")
    @Operation(
            summary = "Submit vocational interests test answers",
            description = "Processes the vocational interests test responses and calculates affinity percentages."
    )
    public ResponseEntity<EvaluationResultResponse> submitVocationalInterestsTest(
            @RequestParam Integer userId,
            @Valid @RequestBody SubmitTestRequest request) {
        EvaluationResultResponse response = evaluationService.submitVocationalInterestsTest(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/evaluations/history"))
                .body(response);
    }

    @GetMapping("/cognitive-skills-test")
    @Operation(
            summary = "Get cognitive skills test",
            description = "Retrieves the questions for the cognitive skills test."
    )
    public TestResponse getCognitiveSkillsTest() {
        return evaluationService.getCognitiveSkillsTest();
    }

    @PostMapping("/cognitive-skills-test")
    @Operation(
            summary = "Submit cognitive skills test answers",
            description = "Processes the cognitive skills test responses and calculates scores."
    )
    public ResponseEntity<EvaluationResultResponse> submitCognitiveSkillsTest(
            @RequestParam Integer userId,
            @Valid @RequestBody SubmitTestRequest request) {
        EvaluationResultResponse response = evaluationService.submitCognitiveSkillsTest(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/evaluations/history"))
                .body(response);
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get evaluation history",
            description = "Retrieves the complete history of evaluations completed by the user."
    )
    public List<EvaluationHistoryResponse> getEvaluationHistory(@RequestParam Integer userId) {
        return evaluationService.getEvaluationHistory(userId);
    }

    @GetMapping("/details/{evaluationId}")
    @Operation(
            summary = "Get evaluation detail",
            description = "Retrieves the complete details of a specific evaluation."
    )
    public ResponseEntity<EvaluationDetailResponse> getEvaluationDetail(
            @RequestParam Integer userId,
            @PathVariable Integer evaluationId) {
        EvaluationDetailResponse response = evaluationService.getEvaluationDetail(userId, evaluationId);
        return ResponseEntity.ok(response);
    }
}