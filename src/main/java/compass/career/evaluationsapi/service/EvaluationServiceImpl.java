package compass.career.evaluationsapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import compass.career.evaluationsapi.client.UsersApiClient;
import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.mapper.EvaluationMapper;
import compass.career.evaluationsapi.mapper.TestMapper;
import compass.career.evaluationsapi.model.*;
import compass.career.evaluationsapi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final CompletedEvaluationRepository completedEvaluationRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final AreaResultRepository areaResultRepository;
    private final VocationalAreaRepository vocationalAreaRepository;
    private final AzureCognitiveService azureCognitiveService;
    private final UsersApiClient usersApiClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public TestResponse getPersonalityTest() {
        Test test = testRepository.findByTestTypeNameAndActiveTrue("personality")
                .orElseThrow(() -> new EntityNotFoundException("Personality test not found"));

        List<Question> randomQuestions = questionRepository
                .findRandomActiveQuestionsByTestId(test.getId(), test.getQuestionsToShow());

        Test testWithRandomQuestions = new Test();
        testWithRandomQuestions.setId(test.getId());
        testWithRandomQuestions.setName(test.getName());
        testWithRandomQuestions.setDescription(test.getDescription());
        testWithRandomQuestions.setTestType(test.getTestType());
        testWithRandomQuestions.setQuestionsToShow(test.getQuestionsToShow());
        testWithRandomQuestions.setQuestions(randomQuestions);

        return TestMapper.toResponse(testWithRandomQuestions);
    }

    @Override
    @Transactional
    public EvaluationResultResponse submitPersonalityTest(Integer userId, SubmitTestRequest request) {
        // Validar que el usuario existe
        if (!usersApiClient.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        if (!"personality".equals(test.getTestType().getName())) {
            throw new IllegalArgumentException("Invalid test type");
        }

        if (request.getAnswers().size() != test.getQuestionsToShow()) {
            throw new IllegalArgumentException("All questions must be answered");
        }

        CompletedEvaluation evaluation = new CompletedEvaluation();
        evaluation.setUserId(userId);
        evaluation.setTest(test);
        evaluation.setCompletionDate(LocalDateTime.now());
        evaluation = completedEvaluationRepository.save(evaluation);

        Map<String, Object> responses = new HashMap<>();
        for (UserAnswerRequest answerReq : request.getAnswers()) {
            Question question = questionRepository.findById(answerReq.getQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException("Question not found"));

            AnswerOption option = answerOptionRepository.findById(answerReq.getOptionId())
                    .orElseThrow(() -> new EntityNotFoundException("Answer option not found"));

            UserAnswer userAnswer = new UserAnswer();
            userAnswer.setEvaluation(evaluation);
            userAnswer.setQuestion(question);
            userAnswer.setOption(option);
            userAnswerRepository.save(userAnswer);

            responses.put("Q" + question.getId(), option.getOptionText());
        }

        Map<String, Object> personalityAnalysis = azureCognitiveService.analyzePersonality(responses);

        BigDecimal totalScore = calculateAverageScore(personalityAnalysis);
        evaluation.setTotalScore(totalScore);
        evaluation = completedEvaluationRepository.save(evaluation);

        EvaluationResult result = new EvaluationResult();
        result.setEvaluation(evaluation);
        try {
            result.setResultJson(objectMapper.writeValueAsString(personalityAnalysis));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing result", e);
        }
        EvaluationResult savedResult = evaluationResultRepository.save(result);

        evaluation.setEvaluationResult(savedResult);

        return EvaluationMapper.toResultResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getVocationalInterestsTest() {
        Test test = testRepository.findByTestTypeNameAndActiveTrue("vocational_interests")
                .orElseThrow(() -> new EntityNotFoundException("Vocational interests test not found"));

        List<Question> randomQuestions = questionRepository
                .findRandomActiveQuestionsByTestId(test.getId(), test.getQuestionsToShow());

        Test testWithRandomQuestions = new Test();
        testWithRandomQuestions.setId(test.getId());
        testWithRandomQuestions.setName(test.getName());
        testWithRandomQuestions.setDescription(test.getDescription());
        testWithRandomQuestions.setTestType(test.getTestType());
        testWithRandomQuestions.setQuestionsToShow(test.getQuestionsToShow());
        testWithRandomQuestions.setQuestions(randomQuestions);

        return TestMapper.toResponse(testWithRandomQuestions);
    }

    @Override
    @Transactional
    public EvaluationResultResponse submitVocationalInterestsTest(Integer userId, SubmitTestRequest request) {
        // Validar que el usuario existe
        if (!usersApiClient.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        if (!"vocational_interests".equals(test.getTestType().getName())) {
            throw new IllegalArgumentException("Invalid test type");
        }

        if (request.getAnswers().size() != test.getQuestionsToShow()) {
            throw new IllegalArgumentException("All questions must be answered");
        }

        CompletedEvaluation evaluation = new CompletedEvaluation();
        evaluation.setUserId(userId);
        evaluation.setTest(test);
        evaluation.setCompletionDate(LocalDateTime.now());
        evaluation = completedEvaluationRepository.save(evaluation);

        Map<String, Integer> areaScores = new HashMap<>();

        for (UserAnswerRequest answerReq : request.getAnswers()) {
            Question question = questionRepository.findById(answerReq.getQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException("Question not found"));

            AnswerOption option = answerOptionRepository.findById(answerReq.getOptionId())
                    .orElseThrow(() -> new EntityNotFoundException("Answer option not found"));

            UserAnswer userAnswer = new UserAnswer();
            userAnswer.setEvaluation(evaluation);
            userAnswer.setQuestion(question);
            userAnswer.setOption(option);
            userAnswerRepository.save(userAnswer);

            if (option.getCategory() != null) {
                areaScores.put(option.getCategory(),
                        areaScores.getOrDefault(option.getCategory(), 0) +
                                (option.getWeightValue() != null ? option.getWeightValue() : 0));
            }
        }

        int totalScore = areaScores.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, BigDecimal> areaPercentages = new HashMap<>();

        for (Map.Entry<String, Integer> entry : areaScores.entrySet()) {
            BigDecimal percentage = totalScore > 0 ?
                    BigDecimal.valueOf(entry.getValue() * 100.0 / totalScore).setScale(2, BigDecimal.ROUND_HALF_UP) :
                    BigDecimal.ZERO;
            areaPercentages.put(entry.getKey(), percentage);
        }

        List<Map.Entry<String, BigDecimal>> sortedAreas = areaPercentages.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        int ranking = 1;
        for (Map.Entry<String, BigDecimal> entry : sortedAreas) {
            VocationalArea area = vocationalAreaRepository.findByName(entry.getKey())
                    .orElseGet(() -> {
                        VocationalArea newArea = new VocationalArea();
                        newArea.setName(entry.getKey());
                        return vocationalAreaRepository.save(newArea);
                    });

            AreaResult areaResult = new AreaResult();
            areaResult.setEvaluation(evaluation);
            areaResult.setVocationalArea(area);
            areaResult.setPercentage(entry.getValue());
            areaResult.setRanking(ranking++);
            areaResultRepository.save(areaResult);
        }

        BigDecimal avgScore = sortedAreas.isEmpty() ? BigDecimal.ZERO :
                sortedAreas.stream()
                        .map(Map.Entry::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(sortedAreas.size()), 2, BigDecimal.ROUND_HALF_UP);

        evaluation.setTotalScore(avgScore);
        evaluation = completedEvaluationRepository.save(evaluation);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("topAreas", sortedAreas.stream()
                .map(e -> Map.of("area", e.getKey(), "percentage", e.getValue()))
                .collect(Collectors.toList()));
        resultData.put("recommendations", generateVocationalRecommendations(sortedAreas));

        EvaluationResult result = new EvaluationResult();
        result.setEvaluation(evaluation);
        try {
            result.setResultJson(objectMapper.writeValueAsString(resultData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing result", e);
        }
        EvaluationResult savedResult = evaluationResultRepository.save(result);

        evaluation.setEvaluationResult(savedResult);

        return EvaluationMapper.toResultResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getCognitiveSkillsTest() {
        Test test = testRepository.findByTestTypeNameAndActiveTrue("cognitive_skills")
                .orElseThrow(() -> new EntityNotFoundException("Cognitive skills test not found"));

        List<Question> randomQuestions = questionRepository
                .findRandomActiveQuestionsByTestId(test.getId(), test.getQuestionsToShow());

        Test testWithRandomQuestions = new Test();
        testWithRandomQuestions.setId(test.getId());
        testWithRandomQuestions.setName(test.getName());
        testWithRandomQuestions.setDescription(test.getDescription());
        testWithRandomQuestions.setTestType(test.getTestType());
        testWithRandomQuestions.setQuestionsToShow(test.getQuestionsToShow());
        testWithRandomQuestions.setQuestions(randomQuestions);

        return TestMapper.toResponse(testWithRandomQuestions);
    }

    @Override
    @Transactional
    public EvaluationResultResponse submitCognitiveSkillsTest(Integer userId, SubmitTestRequest request) {
        // Validar que el usuario existe
        if (!usersApiClient.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        if (!"cognitive_skills".equals(test.getTestType().getName())) {
            throw new IllegalArgumentException("Invalid test type");
        }

        if (request.getAnswers().size() != test.getQuestionsToShow()) {
            throw new IllegalArgumentException("All questions must be answered");
        }

        CompletedEvaluation evaluation = new CompletedEvaluation();
        evaluation.setUserId(userId);
        evaluation.setTest(test);
        evaluation.setCompletionDate(LocalDateTime.now());
        evaluation = completedEvaluationRepository.save(evaluation);

        Map<String, Integer> cognitiveScores = new HashMap<>();
        Map<String, Integer> cognitiveMaxScores = new HashMap<>();

        for (UserAnswerRequest answerReq : request.getAnswers()) {
            Question question = questionRepository.findById(answerReq.getQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException("Question not found"));

            AnswerOption option = answerOptionRepository.findById(answerReq.getOptionId())
                    .orElseThrow(() -> new EntityNotFoundException("Answer option not found"));

            UserAnswer userAnswer = new UserAnswer();
            userAnswer.setEvaluation(evaluation);
            userAnswer.setQuestion(question);
            userAnswer.setOption(option);
            userAnswerRepository.save(userAnswer);

            if (option.getCategory() != null && option.getWeightValue() != null) {
                String category = option.getCategory();
                cognitiveScores.put(category,
                        cognitiveScores.getOrDefault(category, 0) + option.getWeightValue());

                int maxWeight = question.getAnswerOptions().stream()
                        .filter(o -> category.equals(o.getCategory()))
                        .mapToInt(o -> o.getWeightValue() != null ? o.getWeightValue() : 0)
                        .max()
                        .orElse(0);

                cognitiveMaxScores.put(category,
                        cognitiveMaxScores.getOrDefault(category, 0) + maxWeight);
            }
        }

        Map<String, Object> areaScores = new HashMap<>();
        for (String category : cognitiveScores.keySet()) {
            int score = cognitiveScores.get(category);
            int maxScore = cognitiveMaxScores.get(category);
            BigDecimal percentage = maxScore > 0 ?
                    BigDecimal.valueOf(score * 100.0 / maxScore).setScale(2, BigDecimal.ROUND_HALF_UP) :
                    BigDecimal.ZERO;

            String level = determineLevel(percentage);

            Map<String, Object> areaData = new HashMap<>();
            areaData.put("score", percentage);
            areaData.put("level", level);
            areaScores.put(category, areaData);
        }

        BigDecimal totalScore = cognitiveScores.isEmpty() ? BigDecimal.ZERO :
                areaScores.values().stream()
                        .map(v -> ((Map<String, Object>) v).get("score"))
                        .map(s -> (BigDecimal) s)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(areaScores.size()), 2, BigDecimal.ROUND_HALF_UP);

        evaluation.setTotalScore(totalScore);
        evaluation = completedEvaluationRepository.save(evaluation);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("cognitiveAreas", areaScores);
        resultData.put("overallLevel", determineLevel(totalScore));

        EvaluationResult result = new EvaluationResult();
        result.setEvaluation(evaluation);
        try {
            result.setResultJson(objectMapper.writeValueAsString(resultData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing result", e);
        }
        EvaluationResult savedResult = evaluationResultRepository.save(result);

        evaluation.setEvaluationResult(savedResult);

        return EvaluationMapper.toResultResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationHistoryResponse> getEvaluationHistory(Integer userId) {
        List<EvaluationHistoryResponse> history = completedEvaluationRepository
                .findByUserIdOrderByCompletionDateDesc(userId).stream()
                .map(EvaluationMapper::toHistoryResponse)
                .collect(Collectors.toList());

        if (history.isEmpty()) {
            throw new IllegalArgumentException("There is no review history for this user.");
        }

        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationDetailResponse getEvaluationDetail(Integer userId, Integer evaluationId) {
        CompletedEvaluation evaluation = completedEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation not found"));

        if (!evaluation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("This evaluation does not belong to the user");
        }

        List<UserAnswer> userAnswers = userAnswerRepository.findByEvaluationIdOrderByQuestionId(evaluationId);

        List<UserAnswerDetail> answerDetails = userAnswers.stream()
                .map(ua -> UserAnswerDetail.builder()
                        .questionId(ua.getQuestion().getId())
                        .questionText(ua.getQuestion().getQuestionText())
                        .selectedOptionId(ua.getOption().getId())
                        .selectedOptionText(ua.getOption().getOptionText())
                        .category(ua.getOption().getCategory())
                        .weightValue(ua.getOption().getWeightValue())
                        .build())
                .collect(Collectors.toList());

        Object analysisResult = null;
        if (evaluation.getEvaluationResult() != null &&
                evaluation.getEvaluationResult().getResultJson() != null) {
            try {
                analysisResult = objectMapper.readValue(
                        evaluation.getEvaluationResult().getResultJson(),
                        Object.class);
            } catch (JsonProcessingException e) {
                // Si hay error al parsear, simplemente dejamos null
            }
        }

        return EvaluationDetailResponse.builder()
                .evaluationId(evaluation.getId())
                .testName(evaluation.getTest().getName())
                .testType(evaluation.getTest().getTestType().getName())
                .completionDate(evaluation.getCompletionDate())
                .totalScore(evaluation.getTotalScore() != null ?
                        evaluation.getTotalScore().toString() : "N/A")
                .answers(answerDetails)
                .analysisResult(analysisResult)
                .build();
    }

    // Métodos auxiliares

    private BigDecimal calculateAverageScore(Map<String, Object> personalityAnalysis) {
        if (personalityAnalysis == null || !personalityAnalysis.containsKey("dimensions")) {
            return BigDecimal.ZERO;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> dimensions = (Map<String, Object>) personalityAnalysis.get("dimensions");

        if (dimensions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double sum = dimensions.values().stream()
                .mapToDouble(v -> v instanceof Number ? ((Number) v).doubleValue() : 0.0)
                .sum();

        return BigDecimal.valueOf(sum / dimensions.size()).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String determineLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(40)) <= 0) {
            return "bajo";
        } else if (score.compareTo(BigDecimal.valueOf(70)) <= 0) {
            return "medio";
        } else {
            return "alto";
        }
    }

    private List<String> generateVocationalRecommendations(List<Map.Entry<String, BigDecimal>> topAreas) {
        List<String> recommendations = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : topAreas) {
            String area = entry.getKey();
            recommendations.add("Considera carreras en el área de " + area +
                    " donde podrás desarrollar tu potencial al máximo.");
        }

        return recommendations;
    }
}