package compass.career.evaluationsapi.mapper;

import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.model.*;

import java.util.ArrayList;
import java.util.List;

public final class AdminMapper {

    public static TestListResponse toTestListResponse(Test entity) {
        if (entity == null)
            return null;

        return TestListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .testType(entity.getTestType().getName())
                .questionsCount(entity.getQuestions() != null ? entity.getQuestions().size() : 0)
                .active(entity.getActive())
                .build();
    }

    public static Test toTestEntity(CreateTestRequest dto, TestType testType) {
        if (dto == null || testType == null)
            return null;

        Test test = new Test();
        test.setTestType(testType);
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsToShow(dto.getQuestionsToShow());
        test.setActive(true);

        List<Question> questions = new ArrayList<>();
        if (dto.getQuestions() != null) {
            for (QuestionRequest qDto : dto.getQuestions()) {
                Question question = toQuestionEntity(qDto, test);
                questions.add(question);
            }
        }
        test.setQuestions(questions);

        return test;
    }

    public static Question toQuestionEntity(QuestionRequest dto, Test test) {
        if (dto == null || test == null)
            return null;

        Question question = new Question();
        question.setTest(test);
        question.setQuestionText(dto.getQuestionText());
        question.setOrderNumber(dto.getOrderNumber());
        question.setActive(true);

        List<AnswerOption> options = new ArrayList<>();
        if (dto.getOptions() != null) {
            for (AnswerOptionRequest oDto : dto.getOptions()) {
                AnswerOption option = toAnswerOptionEntity(oDto, question);
                options.add(option);
            }
        }
        question.setAnswerOptions(options);

        return question;
    }

    public static AnswerOption toAnswerOptionEntity(AnswerOptionRequest dto, Question question) {
        if (dto == null || question == null)
            return null;

        AnswerOption option = new AnswerOption();
        option.setQuestion(question);
        option.setOptionText(dto.getOptionText());
        option.setWeightValue(dto.getWeightValue());
        option.setCategory(dto.getCategory());

        return option;
    }

    public static CareerResponse toCareerResponse(Career entity) {
        if (entity == null)
            return null;

        return CareerResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .durationSemesters(entity.getDurationSemesters())
                .graduateProfile(entity.getGraduateProfile())
                .jobField(entity.getJobField())
                .averageSalary(entity.getAverageSalary())
                .build();
    }

    public static Career toCareerEntity(CareerRequest dto) {
        if (dto == null)
            return null;

        Career entity = new Career();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDurationSemesters(dto.getDurationSemesters());
        entity.setGraduateProfile(dto.getGraduateProfile());
        entity.setJobField(dto.getJobField());
        entity.setAverageSalary(dto.getAverageSalary());

        return entity;
    }

    public static void copyToCareerEntity(CareerRequest dto, Career entity) {
        if (dto == null || entity == null)
            return;

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDurationSemesters(dto.getDurationSemesters());
        entity.setGraduateProfile(dto.getGraduateProfile());
        entity.setJobField(dto.getJobField());
        entity.setAverageSalary(dto.getAverageSalary());
    }

    public static SpecializationAreaResponse toSpecializationResponse(SpecializationArea entity) {
        if (entity == null)
            return null;

        return SpecializationAreaResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .applicationFields(entity.getApplicationFields())
                .jobProjection(entity.getJobProjection())
                .careerId(entity.getCareer().getId())
                .careerName(entity.getCareer().getName())
                .build();
    }

    public static SpecializationArea toSpecializationEntity(SpecializationAreaRequest dto, Career career) {
        if (dto == null || career == null)
            return null;

        SpecializationArea entity = new SpecializationArea();
        entity.setCareer(career);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setApplicationFields(dto.getApplicationFields());
        entity.setJobProjection(dto.getJobProjection());

        return entity;
    }

    public static void copyToSpecializationEntity(SpecializationAreaRequest dto, SpecializationArea entity, Career career) {
        if (dto == null || entity == null || career == null)
            return;

        entity.setCareer(career);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setApplicationFields(dto.getApplicationFields());
        entity.setJobProjection(dto.getJobProjection());
    }
}