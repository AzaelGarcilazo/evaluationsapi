package compass.career.evaluationsapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AzureCognitiveServiceImpl implements AzureCognitiveService {

    private final RestTemplate restTemplate;

    @Value("${azure.cognitive.endpoint}")
    private String azureEndpoint;

    @Value("${azure.cognitive.api-key}")
    private String azureApiKey;

    @Override
    public Map<String, Object> analyzePersonality(Map<String, Object> responses) {
        StringBuilder combinedText = new StringBuilder();
        for (Map.Entry<String, Object> entry : responses.entrySet()) {
            combinedText.append(entry.getValue().toString()).append(". ");
        }

        Map<String, Object> sentimentResult = analyzeSentiment(combinedText.toString());
        Map<String, Object> keyPhrasesResult = extractKeyPhrases(combinedText.toString());

        return transformToPersonalityDimensions(sentimentResult, keyPhrasesResult, responses);
    }

    @Override
    public Map<String, Object> analyzeText(String text) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> sentimentResult = analyzeSentiment(text);
        result.put("sentiment", sentimentResult);

        Map<String, Object> keyPhrasesResult = extractKeyPhrases(text);
        result.put("keyPhrases", keyPhrasesResult);

        return result;
    }

    private Map<String, Object> analyzeSentiment(String text) {
        String url = azureEndpoint + "/language/:analyze-text?api-version=2023-04-01";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", azureApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("kind", "SentimentAnalysis");

        Map<String, Object> analysisInput = new HashMap<>();
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "1");
        document.put("language", "es");
        document.put("text", text);
        documents.add(document);
        analysisInput.put("documents", documents);
        requestBody.put("analysisInput", analysisInput);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Error analyzing sentiment with Azure: " + e.getMessage());
            e.printStackTrace();
            return getDefaultSentimentResult();
        }
    }

    private Map<String, Object> extractKeyPhrases(String text) {
        String url = azureEndpoint + "/language/:analyze-text?api-version=2023-04-01";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", azureApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("kind", "KeyPhraseExtraction");

        Map<String, Object> analysisInput = new HashMap<>();
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Object> document = new HashMap<>();
        document.put("id", "1");
        document.put("language", "es");
        document.put("text", text);
        documents.add(document);
        analysisInput.put("documents", documents);
        requestBody.put("analysisInput", analysisInput);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Error extracting key phrases with Azure: " + e.getMessage());
            e.printStackTrace();
            return getDefaultKeyPhrasesResult();
        }
    }

    private Map<String, Object> transformToPersonalityDimensions(
            Map<String, Object> sentimentResult,
            Map<String, Object> keyPhrasesResult,
            Map<String, Object> originalResponses) {

        Map<String, Object> result = new HashMap<>();

        double sentimentScore = extractSentimentScore(sentimentResult);
        List<String> keyPhrases = extractKeyPhrasesList(keyPhrasesResult);

        Map<String, Double> dimensions = calculatePersonalityDimensions(
                sentimentScore,
                keyPhrases,
                originalResponses
        );

        result.put("dimensions", dimensions);
        result.put("description", generatePersonalityDescription(dimensions));
        result.put("keyTraits", identifyKeyTraits(dimensions));
        result.put("sentimentAnalysis", sentimentResult);
        result.put("keyPhrases", keyPhrases);

        return result;
    }

    private double extractSentimentScore(Map<String, Object> sentimentResult) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> results = (Map<String, Object>) sentimentResult.get("results");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) results.get("documents");

            if (documents != null && !documents.isEmpty()) {
                Map<String, Object> doc = documents.get(0);

                @SuppressWarnings("unchecked")
                Map<String, Object> confidenceScores = (Map<String, Object>) doc.get("confidenceScores");

                double positive = ((Number) confidenceScores.get("positive")).doubleValue();
                double neutral = ((Number) confidenceScores.get("neutral")).doubleValue();
                double negative = ((Number) confidenceScores.get("negative")).doubleValue();

                return positive - negative;
            }
        } catch (Exception e) {
            System.err.println("Error extracting sentiment score: " + e.getMessage());
        }

        return 0.0;
    }

    private List<String> extractKeyPhrasesList(Map<String, Object> keyPhrasesResult) {
        List<String> phrases = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> results = (Map<String, Object>) keyPhrasesResult.get("results");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) results.get("documents");

            if (documents != null && !documents.isEmpty()) {
                Map<String, Object> doc = documents.get(0);

                @SuppressWarnings("unchecked")
                List<String> keyPhrases = (List<String>) doc.get("keyPhrases");

                if (keyPhrases != null) {
                    phrases.addAll(keyPhrases);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting key phrases list: " + e.getMessage());
        }

        return phrases;
    }

    private Map<String, Double> calculatePersonalityDimensions(
            double sentimentScore,
            List<String> keyPhrases,
            Map<String, Object> originalResponses) {

        Map<String, Double> dimensions = new HashMap<>();

        long conscientiousnessWords = keyPhrases.stream()
                .filter(phrase -> phrase.toLowerCase().matches(".*(organiz|plan|respons|deber|orden).*"))
                .count();
        double conscientiousness = 50.0 + (conscientiousnessWords * 8.0);
        conscientiousness = Math.max(0, Math.min(100, conscientiousness));

        double openness = 50.0 + (keyPhrases.size() * 2.0) + (sentimentScore * 10);
        openness = Math.max(0, Math.min(100, openness));

        long extraversionWords = keyPhrases.stream()
                .filter(phrase -> phrase.toLowerCase().matches(".*(grupo|gente|social|equipo|amig).*"))
                .count();
        double extraversion = 50.0 + (sentimentScore * 15) + (extraversionWords * 7.0);
        extraversion = Math.max(0, Math.min(100, extraversion));

        long agreeablenessWords = keyPhrases.stream()
                .filter(phrase -> phrase.toLowerCase().matches(".*(ayud|cooper|apoyo|equipo|colabor).*"))
                .count();
        double agreeableness = 50.0 + (sentimentScore * 12) + (agreeablenessWords * 8.0);
        agreeableness = Math.max(0, Math.min(100, agreeableness));

        long neuroticismWords = keyPhrases.stream()
                .filter(phrase -> phrase.toLowerCase().matches(".*(estrés|ansi|preocup|nerv|problem).*"))
                .count();
        double neuroticism = 50.0 - (sentimentScore * 15) + (neuroticismWords * 8.0);
        neuroticism = Math.max(0, Math.min(100, neuroticism));

        dimensions.put("openness", Math.round(openness * 100.0) / 100.0);
        dimensions.put("conscientiousness", Math.round(conscientiousness * 100.0) / 100.0);
        dimensions.put("extraversion", Math.round(extraversion * 100.0) / 100.0);
        dimensions.put("agreeableness", Math.round(agreeableness * 100.0) / 100.0);
        dimensions.put("neuroticism", Math.round(neuroticism * 100.0) / 100.0);

        return dimensions;
    }

    private String generatePersonalityDescription(Map<String, Double> dimensions) {
        StringBuilder description = new StringBuilder();

        double openness = dimensions.get("openness");
        double conscientiousness = dimensions.get("conscientiousness");
        double extraversion = dimensions.get("extraversion");
        double agreeableness = dimensions.get("agreeableness");
        double neuroticism = dimensions.get("neuroticism");

        if (openness > 70) {
            description.append("Persona creativa y curiosa, abierta a nuevas experiencias. ");
        } else if (openness < 40) {
            description.append("Persona práctica y tradicional, prefiere lo conocido. ");
        }

        if (conscientiousness > 70) {
            description.append("Muy organizada y responsable, con fuerte sentido del deber. ");
        } else if (conscientiousness < 40) {
            description.append("Flexible y espontánea, prefiere la adaptabilidad. ");
        }

        if (extraversion > 70) {
            description.append("Sociable y energética, disfruta de la interacción con otros. ");
        } else if (extraversion < 40) {
            description.append("Reservada e introspectiva, valora el tiempo a solas. ");
        }

        if (agreeableness > 70) {
            description.append("Cooperativa y empática, busca armonía en las relaciones. ");
        } else if (agreeableness < 40) {
            description.append("Directa y analítica, valora la objetividad. ");
        }

        if (neuroticism > 70) {
            description.append("Sensible emocionalmente, puede experimentar estrés con facilidad.");
        } else if (neuroticism < 40) {
            description.append("Emocionalmente estable y resiliente ante desafíos.");
        }

        return description.toString().trim();
    }

    private List<String> identifyKeyTraits(Map<String, Double> dimensions) {
        List<String> traits = new ArrayList<>();

        dimensions.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    String trait = translateDimension(entry.getKey());
                    String level = entry.getValue() > 70 ? "Alto" :
                            entry.getValue() > 40 ? "Moderado" : "Bajo";
                    traits.add(level + " " + trait);
                });

        return traits;
    }

    private String translateDimension(String dimension) {
        switch (dimension) {
            case "openness": return "Apertura a experiencias";
            case "conscientiousness": return "Responsabilidad";
            case "extraversion": return "Extroversión";
            case "agreeableness": return "Amabilidad";
            case "neuroticism": return "Estabilidad emocional";
            default: return dimension;
        }
    }

    private Map<String, Object> getDefaultSentimentResult() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Object> document = new HashMap<>();

        Map<String, Object> confidenceScores = new HashMap<>();
        confidenceScores.put("positive", 0.33);
        confidenceScores.put("neutral", 0.34);
        confidenceScores.put("negative", 0.33);

        document.put("confidenceScores", confidenceScores);
        document.put("sentiment", "neutral");
        documents.add(document);
        results.put("documents", documents);
        result.put("results", results);

        return result;
    }

    private Map<String, Object> getDefaultKeyPhrasesResult() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Object> document = new HashMap<>();

        document.put("keyPhrases", new ArrayList<>());
        documents.add(document);
        results.put("documents", documents);
        result.put("results", results);

        return result;
    }
}