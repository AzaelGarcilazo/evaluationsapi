package compass.career.evaluationsapi.mapper;

import compass.career.evaluationsapi.dto.AnswerOptionResponse;
import compass.career.evaluationsapi.dto.QuestionResponse;
import compass.career.evaluationsapi.dto.TestResponse;
import compass.career.evaluationsapi.model.AnswerOption;
import compass.career.evaluationsapi.model.Question;
import compass.career.evaluationsapi.model.Test;

import java.util.stream.Collectors;

public final class TestMapper {

    public static TestResponse toResponse(Test entity) {
        if (entity == null)
            return null;

        return TestResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .testType(entity.getTestType().getName())
                .questionsToShow(entity.getQuestionsToShow())
                .questions(entity.getQuestions() != null ?
                        entity.getQuestions().stream()
                                .filter(Question::getActive)
                                .map(TestMapper::toQuestionResponse)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public static QuestionResponse toQuestionResponse(Question entity) {
        if (entity == null)
            return null;

        return QuestionResponse.builder()
                .id(entity.getId())
                .questionText(entity.getQuestionText())
                .orderNumber(entity.getOrderNumber())
                .options(entity.getAnswerOptions() != null ?
                        entity.getAnswerOptions().stream()
                                .map(TestMapper::toAnswerOptionResponse)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public static AnswerOptionResponse toAnswerOptionResponse(AnswerOption entity) {
        if (entity == null)
            return null;

        return AnswerOptionResponse.builder()
                .id(entity.getId())
                .optionText(entity.getOptionText())
                .build();
    }
}