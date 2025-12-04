package compass.career.evaluationsapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GroqService {

    private final OkHttpClient client;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;
    private final ObjectMapper objectMapper;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public GroqService(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.model}") String model,
            @Value("${groq.max-tokens}") Integer maxTokens,
            @Value("${groq.temperature}") Double temperature,
            ObjectMapper objectMapper) {

        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.objectMapper = objectMapper;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("Groq Service initialized with model: {}", model);
    }

    public List<CareerRecommendation> generateCareerRecommendations(
            Map<String, Object> personalityResults,
            Map<String, Object> vocationalResults,
            Map<String, Object> cognitiveResults,
            List<CareerInfo> availableCareers) {

        log.info("Generating career recommendations for {} careers using Groq AI", availableCareers.size());

        String prompt = buildRecommendationPrompt(
                personalityResults,
                vocationalResults,
                cognitiveResults,
                availableCareers
        );

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", "Eres un consejero vocacional experto que analiza perfiles de estudiantes basados en:\n" +
                            "1. Test de Personalidad (Modelo RIASEC)\n" +
                            "2. Test de Intereses Vocacionales\n" +
                            "3. Test de Habilidades Cognitivas\n\n" +
                            "Tu tarea es analizar estos resultados y recomendar las carreras más compatibles.\n" +
                            "IMPORTANTE: Debes devolver SOLO un JSON válido sin texto adicional."
            ));
            messages.add(Map.of(
                    "role", "user",
                    "content", prompt
            ));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Groq API error: " + response.code() + " - " + response.body().string());
                }

                String responseBody = response.body().string();
                log.debug("Groq API raw response: {}", responseBody);

                Map<String, Object> jsonResponse = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");

                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                Map<String, Object> contentJson = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recommendations = (List<Map<String, Object>>) contentJson.get("recommendations");

                List<CareerRecommendation> result = new ArrayList<>();
                for (Map<String, Object> rec : recommendations) {
                    CareerRecommendation recommendation = new CareerRecommendation();
                    recommendation.setCareerId(((Number) rec.get("careerId")).intValue());
                    recommendation.setCompatibilityPercentage(((Number) rec.get("compatibilityPercentage")).doubleValue());
                    recommendation.setReason((String) rec.get("reason"));
                    result.add(recommendation);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) jsonResponse.get("usage");
                log.info("Groq API - Total tokens: {}, Prompt tokens: {}, Completion tokens: {}",
                        usage.get("total_tokens"), usage.get("prompt_tokens"), usage.get("completion_tokens"));

                log.info("Successfully generated {} career recommendations", result.size());
                return result;
            }

        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            throw new RuntimeException("Failed to generate career recommendations: " + e.getMessage(), e);
        }
    }

    private String buildRecommendationPrompt(
            Map<String, Object> personalityResults,
            Map<String, Object> vocationalResults,
            Map<String, Object> cognitiveResults,
            List<CareerInfo> availableCareers) {

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

        prompt.append("# CARRERAS DISPONIBLES\n\n");
        for (CareerInfo career : availableCareers) {
            prompt.append(String.format("- **ID:** %d | **Nombre:** %s | **Duración:** %d semestres | **Salario promedio:** $%.2f\n",
                    career.getId(),
                    career.getName(),
                    career.getDurationSemesters(),
                    career.getAverageSalary() != null ? career.getAverageSalary().doubleValue() : 0.0
            ));
            if (career.getDescription() != null && !career.getDescription().isEmpty()) {
                prompt.append(String.format("  Descripción: %s\n", career.getDescription()));
            }
        }

        prompt.append("\n# INSTRUCCIONES\n\n");
        prompt.append("1. Analiza la compatibilidad entre el perfil del estudiante y cada carrera disponible\n");
        prompt.append("2. Considera TODOS los factores: personalidad RIASEC, intereses vocacionales y habilidades cognitivas\n");
        prompt.append("3. Calcula un porcentaje de compatibilidad (0-100) para cada carrera\n");
        prompt.append("4. El porcentaje debe reflejar qué tan bien el perfil del estudiante encaja con los requisitos típicos de cada carrera\n");
        prompt.append("5. Ordena las carreras de mayor a menor compatibilidad\n");
        prompt.append("6. Devuelve SOLO las TOP 10 carreras más compatibles\n");
        prompt.append("7. Para cada carrera incluye una razón específica y personalizada (2-3 oraciones) explicando por qué es compatible\n\n");

        prompt.append("# FORMATO DE RESPUESTA\n\n");
        prompt.append("Devuelve ÚNICAMENTE un JSON con este formato exacto:\n\n");
        prompt.append("{\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"careerId\": 1,\n");
        prompt.append("      \"compatibilityPercentage\": 95.5,\n");
        prompt.append("      \"reason\": \"Tu alto puntaje en Investigador (92%) combinado con tu interés en Ciencias Exactas (88%) y tus fuertes habilidades de razonamiento lógico (85%) indican una excelente compatibilidad con esta carrera.\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    // Clases internas
    public static class CareerInfo {
        private Integer id;
        private String name;
        private String description;
        private Integer durationSemesters;
        private java.math.BigDecimal averageSalary;

        public CareerInfo(Integer id, String name, String description, Integer durationSemesters, java.math.BigDecimal averageSalary) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.durationSemesters = durationSemesters;
            this.averageSalary = averageSalary;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Integer getDurationSemesters() { return durationSemesters; }
        public java.math.BigDecimal getAverageSalary() { return averageSalary; }
    }

    public static class CareerRecommendation {
        private Integer careerId;
        private Double compatibilityPercentage;
        private String reason;

        public Integer getCareerId() { return careerId; }
        public void setCareerId(Integer careerId) { this.careerId = careerId; }

        public Double getCompatibilityPercentage() { return compatibilityPercentage; }
        public void setCompatibilityPercentage(Double compatibilityPercentage) {
            this.compatibilityPercentage = compatibilityPercentage;
        }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public String callGroqAPI(String userPrompt, String systemPrompt) throws Exception {
        log.info("Calling Groq API with generic prompt");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("response_format", Map.of("type", "json_object"));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Groq API error: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            log.debug("Groq API raw response: {}", responseBody);

            Map<String, Object> jsonResponse = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) jsonResponse.get("usage");
            log.info("Groq API - Total tokens: {}, Prompt tokens: {}, Completion tokens: {}",
                    usage.get("total_tokens"), usage.get("prompt_tokens"), usage.get("completion_tokens"));

            return (String) message.get("content");
        }
    }
}