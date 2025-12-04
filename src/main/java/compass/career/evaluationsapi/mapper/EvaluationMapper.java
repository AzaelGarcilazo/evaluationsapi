package compass.career.evaluationsapi.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import compass.career.evaluationsapi.dto.EvaluationHistoryResponse;
import compass.career.evaluationsapi.dto.EvaluationResultResponse;
import compass.career.evaluationsapi.model.CompletedEvaluation;

public final class EvaluationMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static EvaluationHistoryResponse toHistoryResponse(CompletedEvaluation entity) {
        if (entity == null)
            return null;

        return EvaluationHistoryResponse.builder()
                .id(entity.getId())
                .testName(entity.getTest().getName())
                .testType(entity.getTest().getTestType().getName())
                .completionDate(entity.getCompletionDate())
                .totalScore(entity.getTotalScore())
                .build();
    }

    public static EvaluationResultResponse toResultResponse(CompletedEvaluation entity) {
        if (entity == null)
            return null;

        Object resultDetails = null;
        if (entity.getEvaluationResult() != null) {
            try {
                resultDetails = objectMapper.readValue(
                        entity.getEvaluationResult().getResultJson(),
                        Object.class
                );
            } catch (JsonProcessingException e) {
                resultDetails = entity.getEvaluationResult().getResultJson();
            }
        }

        return EvaluationResultResponse.builder()
                .evaluationId(entity.getId())
                .testName(entity.getTest().getName())
                .testType(entity.getTest().getTestType().getName())
                .completionDate(entity.getCompletionDate())
                .totalScore(entity.getTotalScore())
                .resultDetails(resultDetails)
                .build();
    }
}