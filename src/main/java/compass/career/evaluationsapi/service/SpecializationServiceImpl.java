package compass.career.evaluationsapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import compass.career.evaluationsapi.client.UsersApiClient;
import compass.career.evaluationsapi.dto.*;
import compass.career.evaluationsapi.mapper.AdminMapper;
import compass.career.evaluationsapi.mapper.SpecializationMapper;
import compass.career.evaluationsapi.model.*;
import compass.career.evaluationsapi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecializationServiceImpl implements SpecializationService {

    private final SpecializationAreaRepository specializationAreaRepository;
    private final SpecializationRecommendationRepository specializationRecommendationRepository;
    private final CompletedEvaluationRepository completedEvaluationRepository;
    private final SocialMediaApiService socialMediaApiService;
    private final CareerRepository careerRepository;
    private final GroqService groqService;
    private final UsersApiClient usersApiClient;
    private final ObjectMapper objectMapper;

    // Caché simple de recomendaciones (1 hora)
    private final Map<Integer, CachedSpecializationRecommendations> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public List<SpecializationRecommendationResponse> getRecommendedSpecializations(Integer userId) {
        log.info("Generating specialization recommendations for user {}", userId);

        // Validar que el usuario existe
        if (!usersApiClient.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        // Verificar caché
        CachedSpecializationRecommendations cached = cache.get(userId);
        if (cached != null && !cached.isExpired()) {
            log.info("Returning cached specialization recommendations for user {}", userId);
            return cached.getRecommendations();
        }

        // Verificar si ya existen recomendaciones en BD
        List<SpecializationRecommendation> existingRecommendations =
                specializationRecommendationRepository.findByUserIdOrderByCompatibilityPercentageDesc(userId);

        if (!existingRecommendations.isEmpty()) {
            log.info("Found {} existing specialization recommendations in database for user {}",
                    existingRecommendations.size(), userId);
            List<SpecializationRecommendationResponse> responses = existingRecommendations.stream()
                    .map(SpecializationMapper::toRecommendationResponse)
                    .collect(Collectors.toList());

            cache.put(userId, new CachedSpecializationRecommendations(responses));
            return responses;
        }

        // Generar nuevas recomendaciones usando Groq AI
        log.info("Generating NEW specialization recommendations using Groq AI for user {}", userId);

        // 1. Obtener resultados de los tests
        Map<String, Object> personalityResults = getTestResults(userId, "personality");
        Map<String, Object> vocationalResults = getTestResults(userId, "vocational_interests");
        Map<String, Object> cognitiveResults = getTestResults(userId, "cognitive_skills");

        if (personalityResults.isEmpty() && vocationalResults.isEmpty() && cognitiveResults.isEmpty()) {
            throw new IllegalStateException("User must complete at least one evaluation to get recommendations");
        }

        // 2. Obtener habilidades del usuario desde UsersAPI
        SkillsDTO userSkillsDTO = usersApiClient.getUserSkills(userId);
        Map<String, Integer> userSkills = userSkillsDTO != null ? userSkillsDTO.getSkills() : new HashMap<>();

        // 3. Obtener todas las especializaciones disponibles
        List<SpecializationArea> allSpecializations = specializationAreaRepository.findAll();
        if (allSpecializations.isEmpty()) {
            throw new IllegalStateException("No specialization areas available in the system");
        }

        // 4. Preparar información de especializaciones para Groq AI
        List<SpecializationInfo> specializationInfoList = allSpecializations.stream()
                .map(s -> new SpecializationInfo(
                        s.getId(),
                        s.getName(),
                        s.getDescription(),
                        s.getApplicationFields(),
                        s.getJobProjection(),
                        s.getCareer() != null ? s.getCareer().getName() : "General"
                ))
                .collect(Collectors.toList());

        // 5. Llamar a Groq AI para generar recomendaciones
        List<SpecializationRecommendationResult> aiRecommendations;
        try {
            aiRecommendations = generateSpecializationRecommendations(
                    personalityResults,
                    vocationalResults,
                    cognitiveResults,
                    userSkills,
                    specializationInfoList
            );
        } catch (Exception e) {
            log.error("Error generating specialization recommendations with Groq AI", e);
            throw new RuntimeException("Failed to generate specialization recommendations", e);
        }

        // 6. Guardar recomendaciones en la base de datos
        List<SpecializationRecommendation> savedRecommendations = new ArrayList<>();
        for (SpecializationRecommendationResult aiRec : aiRecommendations) {
            SpecializationArea specialization = specializationAreaRepository.findById(aiRec.getSpecializationId())
                    .orElseThrow(() -> new EntityNotFoundException("Specialization not found: " + aiRec.getSpecializationId()));

            SpecializationRecommendation recommendation = new SpecializationRecommendation();
            recommendation.setUserId(userId);
            recommendation.setSpecializationArea(specialization);
            recommendation.setCompatibilityPercentage(BigDecimal.valueOf(aiRec.getCompatibilityPercentage()));

            savedRecommendations.add(specializationRecommendationRepository.save(recommendation));
        }

        // 7. Construir respuesta
        List<SpecializationRecommendationResponse> responses = savedRecommendations.stream()
                .map(SpecializationMapper::toRecommendationResponse)
                .collect(Collectors.toList());

        // 8. Guardar en caché
        cache.put(userId, new CachedSpecializationRecommendations(responses));

        log.info("Successfully generated and saved {} specialization recommendations for user {}", responses.size(), userId);
        return responses;
    }

    private List<SpecializationRecommendationResult> generateSpecializationRecommendations(
            Map<String, Object> personalityResults,
            Map<String, Object> vocationalResults,
            Map<String, Object> cognitiveResults,
            Map<String, Integer> userSkills,
            List<SpecializationInfo> availableSpecializations) {

        log.info("Calling Groq AI for {} specializations", availableSpecializations.size());

        String prompt = buildSpecializationPrompt(
                personalityResults,
                vocationalResults,
                cognitiveResults,
                userSkills,
                availableSpecializations
        );

        try {
            String response = groqService.callGroqAPI(prompt,
                    "Eres un consejero académico experto que analiza perfiles de estudiantes para recomendar " +
                            "especializaciones académicas. Debes considerar los resultados de tests vocacionales, " +
                            "habilidades actuales del estudiante y las características de cada especialización. " +
                            "IMPORTANTE: Debes devolver SOLO un JSON válido sin texto adicional."
            );

            Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) jsonResponse.get("recommendations");

            List<SpecializationRecommendationResult> result = new ArrayList<>();
            for (Map<String, Object> rec : recommendations) {
                SpecializationRecommendationResult recommendation = new SpecializationRecommendationResult();
                recommendation.setSpecializationId(((Number) rec.get("specializationId")).intValue());
                recommendation.setCompatibilityPercentage(((Number) rec.get("compatibilityPercentage")).doubleValue());
                recommendation.setReason((String) rec.get("reason"));
                result.add(recommendation);
            }

            return result;

        } catch (Exception e) {
            log.error("Error parsing Groq AI response for specializations", e);
            throw new RuntimeException("Failed to parse specialization recommendations", e);
        }
    }

    private String buildSpecializationPrompt(
            Map<String, Object> personalityResults,
            Map<String, Object> vocationalResults,
            Map<String, Object> cognitiveResults,
            Map<String, Integer> userSkills,
            List<SpecializationInfo> availableSpecializations) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("# PERFIL DEL ESTUDIANTE\n\n");

        if (personalityResults != null && !personalityResults.isEmpty()) {
            prompt.append("## 1. Test de Personalidad (Modelo RIASEC):\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> dimensions = (Map<String, Object>) personalityResults.get("dimensions");
            if (dimensions != null) {
                dimensions.forEach((category, score) ->
                        prompt.append(String.format("- %s: %.1f%%\n", category, ((Number) score).doubleValue()))
                );
            }
            prompt.append("\n");
        }

        if (vocationalResults != null && !vocationalResults.isEmpty()) {
            prompt.append("## 2. Test de Intereses Vocacionales:\n");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> topAreas = (List<Map<String, Object>>) vocationalResults.get("topAreas");
            if (topAreas != null) {
                for (Map<String, Object> area : topAreas) {
                    prompt.append(String.format("- %s: %.1f%%\n",
                            area.get("area"),
                            ((Number) area.get("percentage")).doubleValue()));
                }
            }
            prompt.append("\n");
        }

        if (cognitiveResults != null && !cognitiveResults.isEmpty()) {
            prompt.append("## 3. Test de Habilidades Cognitivas:\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> cognitiveAreas = (Map<String, Object>) cognitiveResults.get("cognitiveAreas");
            if (cognitiveAreas != null) {
                cognitiveAreas.forEach((area, data) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> areaData = (Map<String, Object>) data;
                    prompt.append(String.format("- %s: %.1f%% (Nivel: %s)\n",
                            area,
                            ((Number) areaData.get("score")).doubleValue(),
                            areaData.get("level")));
                });
            }
            prompt.append("\n");
        }

        if (!userSkills.isEmpty()) {
            prompt.append("## 4. Habilidades Actuales del Estudiante:\n");
            userSkills.forEach((skill, level) ->
                    prompt.append(String.format("- %s: Nivel %d/5\n", skill, level))
            );
            prompt.append("\n");
        }

        prompt.append("# ESPECIALIZACIONES ACADÉMICAS DISPONIBLES\n\n");
        for (SpecializationInfo spec : availableSpecializations) {
            prompt.append(String.format("- **ID:** %d | **Nombre:** %s | **Carrera:** %s\n",
                    spec.getId(),
                    spec.getName(),
                    spec.getCareerName()
            ));
            if (spec.getDescription() != null && !spec.getDescription().isEmpty()) {
                prompt.append(String.format("  Descripción: %s\n", spec.getDescription()));
            }
            if (spec.getApplicationFields() != null && !spec.getApplicationFields().isEmpty()) {
                prompt.append(String.format("  Campos de aplicación: %s\n", spec.getApplicationFields()));
            }
            if (spec.getJobProjection() != null && !spec.getJobProjection().isEmpty()) {
                prompt.append(String.format("  Proyección laboral: %s\n", spec.getJobProjection()));
            }
            prompt.append("\n");
        }

        prompt.append("# INSTRUCCIONES\n\n");
        prompt.append("1. Analiza la compatibilidad entre el perfil del estudiante y cada especialización disponible\n");
        prompt.append("2. Considera TODOS los factores:\n");
        prompt.append("   - Personalidad RIASEC\n");
        prompt.append("   - Intereses vocacionales\n");
        prompt.append("   - Habilidades cognitivas\n");
        prompt.append("   - Habilidades técnicas actuales del estudiante\n");
        prompt.append("3. Calcula un porcentaje de compatibilidad (0-100) para cada especialización\n");
        prompt.append("4. Una especialización es compatible si:\n");
        prompt.append("   - Se alinea con los intereses vocacionales del estudiante\n");
        prompt.append("   - Las habilidades actuales del estudiante son útiles o pueden desarrollarse en esa área\n");
        prompt.append("   - El perfil de personalidad encaja con el tipo de trabajo en esa especialización\n");
        prompt.append("5. Ordena las especializaciones de mayor a menor compatibilidad\n");
        prompt.append("6. Devuelve SOLO las TOP 10 especializaciones más compatibles\n");
        prompt.append("7. Para cada especialización incluye una razón específica y personalizada (2-3 oraciones)\n\n");

        prompt.append("# FORMATO DE RESPUESTA\n\n");
        prompt.append("Devuelve ÚNICAMENTE un JSON con este formato exacto:\n\n");
        prompt.append("{\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"specializationId\": 1,\n");
        prompt.append("      \"compatibilityPercentage\": 94.5,\n");
        prompt.append("      \"reason\": \"Tu fuerte inclinación hacia el análisis lógico (85%) y tus habilidades en programación (nivel 4/5) te hacen ideal para esta especialización.\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private Map<String, Object> getTestResults(Integer userId, String testTypeName) {
        List<CompletedEvaluation> evaluations = completedEvaluationRepository.findByUserIdOrderByCompletionDateDesc(userId);

        Optional<CompletedEvaluation> relevantEvaluation = evaluations.stream()
                .filter(e -> testTypeName.equals(e.getTest().getTestType().getName()))
                .findFirst();

        if (relevantEvaluation.isEmpty()) {
            log.debug("User {} has not completed {} test", userId, testTypeName);
            return Collections.emptyMap();
        }

        CompletedEvaluation evaluation = relevantEvaluation.get();
        if (evaluation.getEvaluationResult() == null) {
            log.warn("No result found for evaluation {}", evaluation.getId());
            return Collections.emptyMap();
        }

        try {
            String jsonResult = evaluation.getEvaluationResult().getResultJson();
            return objectMapper.readValue(jsonResult, Map.class);
        } catch (Exception e) {
            log.error("Error parsing {} test results for user {}", testTypeName, userId, e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SpecializationDetailResponse getSpecializationDetails(Integer specializationId) {
        SpecializationArea specialization = specializationAreaRepository.findById(specializationId)
                .orElseThrow(() -> new EntityNotFoundException("Specialization area not found"));

        Object socialMediaData = socialMediaApiService.getSpecializationInformation(specialization.getName());

        return SpecializationMapper.toDetailResponse(specialization, socialMediaData);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecializationAreaResponse> getAllSpecializations() {
        List<SpecializationAreaResponse> specializations = specializationAreaRepository.findAll().stream()
                .map(AdminMapper::toSpecializationResponse)
                .collect(Collectors.toList());

        if (specializations.isEmpty()) {
            throw new IllegalArgumentException("No hay especializaciones disponibles en el sistema");
        }

        return specializations;
    }

    @Override
    @Transactional(readOnly = true)
    public SpecializationAreaResponse getSpecializationById(Integer specializationId) {
        SpecializationArea specialization = specializationAreaRepository.findById(specializationId)
                .orElseThrow(() -> new EntityNotFoundException("The requested specialization area has not been found"));

        return AdminMapper.toSpecializationResponse(specialization);
    }

    @Override
    @Transactional
    public SpecializationAreaResponse createSpecialization(SpecializationAreaRequest request) {
        Career career = careerRepository.findById(request.getCareerId())
                .orElseThrow(() -> new EntityNotFoundException("The entered career has not been found"));

        SpecializationArea specialization = AdminMapper.toSpecializationEntity(request, career);
        SpecializationArea saved = specializationAreaRepository.save(specialization);
        return AdminMapper.toSpecializationResponse(saved);
    }

    @Override
    @Transactional
    public SpecializationAreaResponse updateSpecialization(Integer specializationId, SpecializationAreaRequest request) {
        SpecializationArea specialization = specializationAreaRepository.findById(specializationId)
                .orElseThrow(() -> new EntityNotFoundException("The specialization area to update has not been found"));

        Career career = careerRepository.findById(request.getCareerId())
                .orElseThrow(() -> new EntityNotFoundException("The entered career has not been found"));

        AdminMapper.copyToSpecializationEntity(request, specialization, career);
        SpecializationArea saved = specializationAreaRepository.save(specialization);
        return AdminMapper.toSpecializationResponse(saved);
    }

    // Clases internas
    private static class CachedSpecializationRecommendations {
        private final List<SpecializationRecommendationResponse> recommendations;
        private final LocalDateTime expiresAt;

        public CachedSpecializationRecommendations(List<SpecializationRecommendationResponse> recommendations) {
            this.recommendations = recommendations;
            this.expiresAt = LocalDateTime.now().plusHours(1);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public List<SpecializationRecommendationResponse> getRecommendations() {
            return recommendations;
        }
    }

    private static class SpecializationInfo {
        private Integer id;
        private String name;
        private String description;
        private String applicationFields;
        private String jobProjection;
        private String careerName;

        public SpecializationInfo(Integer id, String name, String description,
                                  String applicationFields, String jobProjection, String careerName) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.applicationFields = applicationFields;
            this.jobProjection = jobProjection;
            this.careerName = careerName;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getApplicationFields() { return applicationFields; }
        public String getJobProjection() { return jobProjection; }
        public String getCareerName() { return careerName; }
    }

    private static class SpecializationRecommendationResult {
        private Integer specializationId;
        private Double compatibilityPercentage;
        private String reason;

        public Integer getSpecializationId() { return specializationId; }
        public void setSpecializationId(Integer specializationId) { this.specializationId = specializationId; }

        public Double getCompatibilityPercentage() { return compatibilityPercentage; }
        public void setCompatibilityPercentage(Double compatibilityPercentage) {
            this.compatibilityPercentage = compatibilityPercentage;
        }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}