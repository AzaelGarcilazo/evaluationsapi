package compass.career.evaluationsapi.service;

import compass.career.evaluationsapi.dto.CreateTestRequest;
import compass.career.evaluationsapi.dto.TestListResponse;
import compass.career.evaluationsapi.dto.TestResponse;
import compass.career.evaluationsapi.mapper.AdminMapper;
import compass.career.evaluationsapi.mapper.TestMapper;
import compass.career.evaluationsapi.model.Question;
import compass.career.evaluationsapi.model.Test;
import compass.career.evaluationsapi.model.TestType;
import compass.career.evaluationsapi.repository.QuestionRepository;
import compass.career.evaluationsapi.repository.TestRepository;
import compass.career.evaluationsapi.repository.TestTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestServiceImpl implements TestService {

    private final TestRepository testRepository;
    private final TestTypeRepository testTypeRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TestListResponse> getAllTests() {
        List<TestListResponse> tests = testRepository.findAll().stream()
                .map(AdminMapper::toTestListResponse)
                .collect(Collectors.toList());

        if (tests.isEmpty()) {
            throw new IllegalArgumentException("No hay tests disponibles en el sistema");
        }

        return tests;
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTestDetails(Integer testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("The requested test has not been found"));

        // Cargar todas las preguntas activas (no aleatorias para admin)
        List<Question> questions = questionRepository.findByTestIdAndActiveTrue(test.getId());
        test.setQuestions(questions);

        return TestMapper.toResponse(test);
    }

    @Override
    @Transactional
    public TestResponse createTest(CreateTestRequest request) {
        // Validar mínimo 100 preguntas
        if (request.getQuestions() == null || request.getQuestions().size() < 100) {
            throw new IllegalArgumentException("Test must have at least 100 questions");
        }

        TestType testType = testTypeRepository.findById(request.getTestTypeId())
                .orElseThrow(() -> new EntityNotFoundException("The test type has not been found"));

        // ========================================
        // ✅ NUEVA VALIDACIÓN: Un solo test activo por tipo
        // ========================================
        Optional<Test> existingActiveTest = testRepository.findByTestTypeIdAndActiveTrue(testType.getId());

        if (existingActiveTest.isPresent()) {
            Test existing = existingActiveTest.get();
            String testTypeName = getTestTypeDisplayName(testType.getName());

            throw new IllegalStateException(
                    String.format(
                            "A test of type '%s' already exists (ID: %d, Name: '%s'). " +
                                    "Only one active test per type is allowed. " +
                                    "Please deactivate or update the existing test instead of creating a new one.",
                            testTypeName,
                            existing.getId(),
                            existing.getName()
                    )
            );
        }

        log.info("Creating new test of type: {} - {}", testType.getName(), request.getName());

        Test test = AdminMapper.toTestEntity(request, testType);
        Test saved = testRepository.save(test);

        log.info("Test created successfully - ID: {}, Type: {}, Questions: {}",
                saved.getId(), testType.getName(), saved.getQuestions().size());

        return TestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TestResponse updateTest(Integer testId, CreateTestRequest request) {
        // Validar mínimo 100 preguntas
        if (request.getQuestions() == null || request.getQuestions().size() < 100) {
            throw new IllegalArgumentException("Test must have at least 100 questions");
        }

        Test existingTest = testRepository.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("The test to update has not been found"));

        TestType testType = testTypeRepository.findById(request.getTestTypeId())
                .orElseThrow(() -> new EntityNotFoundException("The test type has not been found"));

        // ========================================
        // ✅ VALIDACIÓN: Si se cambia el tipo, verificar que no exista otro test activo de ese tipo
        // ========================================
        if (!existingTest.getTestType().getId().equals(testType.getId())) {
            Optional<Test> existingActiveTest = testRepository.findByTestTypeIdAndActiveTrue(testType.getId());

            if (existingActiveTest.isPresent() && !existingActiveTest.get().getId().equals(testId)) {
                Test existing = existingActiveTest.get();
                String testTypeName = getTestTypeDisplayName(testType.getName());

                throw new IllegalStateException(
                        String.format(
                                "Cannot change test type to '%s' because another active test already exists for this type (ID: %d, Name: '%s'). " +
                                        "Only one active test per type is allowed.",
                                testTypeName,
                                existing.getId(),
                                existing.getName()
                        )
                );
            }

            log.info("Changing test type from '{}' to '{}'",
                    existingTest.getTestType().getName(), testType.getName());
        }

        log.info("Updating test ID: {} - New name: {}", testId, request.getName());

        // Actualizar test
        existingTest.setTestType(testType);
        existingTest.setName(request.getName());
        existingTest.setDescription(request.getDescription());
        existingTest.setQuestionsToShow(request.getQuestionsToShow());

        // Desactivar preguntas antiguas
        if (existingTest.getQuestions() != null) {
            for (Question q : existingTest.getQuestions()) {
                q.setActive(false);
            }
        }

        // Agregar nuevas preguntas
        Test newTestData = AdminMapper.toTestEntity(request, testType);
        existingTest.getQuestions().addAll(newTestData.getQuestions());

        Test saved = testRepository.save(existingTest);

        log.info("Test updated successfully - ID: {}, Type: {}, Questions: {}",
                saved.getId(), testType.getName(), saved.getQuestions().size());

        return TestMapper.toResponse(saved);
    }

    /**
     * Convierte el nombre técnico del tipo de test a un nombre legible
     */
    private String getTestTypeDisplayName(String technicalName) {
        switch (technicalName.toLowerCase()) {
            case "personality":
                return "Personality Test";
            case "vocational_interests":
                return "Vocational Interests Test";
            case "cognitive_skills":
                return "Cognitive Skills Test";
            default:
                return technicalName;
        }
    }
}