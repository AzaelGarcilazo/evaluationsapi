package compass.career.evaluationsapi.service;

import lombok.RequiredArgsConstructor;
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
public class SocialMediaApiServiceImpl implements SocialMediaApiService {

    private final RestTemplate restTemplate;
    private static final String REDDIT_BASE_URL = "https://www.reddit.com";

    @Override
    public Object getCareerInformation(String careerName) {
        Map<String, Object> result = new HashMap<>();

        Object generalSearch = searchReddit(careerName + " career opportunities");
        result.put("generalDiscussion", generalSearch);

        Object subredditSearch = searchInSubreddit(careerName, "cscareerquestions");
        result.put("careerAdvice", subredditSearch);

        result.put("summary", generateCareerSummary(generalSearch, subredditSearch, careerName));

        return result;
    }

    @Override
    public Object getSpecializationInformation(String specializationName) {
        Map<String, Object> result = new HashMap<>();

        Object generalSearch = searchReddit(specializationName + " specialization");
        result.put("discussion", generalSearch);

        Object trendsSearch = searchReddit(specializationName + " job market trends");
        result.put("marketTrends", trendsSearch);

        result.put("summary", generateSpecializationSummary(generalSearch, trendsSearch, specializationName));

        return result;
    }

    private Object searchReddit(String query) {
        String url = REDDIT_BASE_URL + "/search.json?q=" +
                query.replace(" ", "+") + "&limit=10&sort=relevance&t=year";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "VocationalGuidanceApp/1.0 (Educational Purpose)");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            return extractRedditData(result);

        } catch (Exception e) {
            System.err.println("Error calling Reddit API: " + e.getMessage());
            e.printStackTrace();
            return getDefaultRedditData();
        }
    }

    private Object searchInSubreddit(String query, String subreddit) {
        String url = REDDIT_BASE_URL + "/r/" + subreddit + "/search.json?q=" +
                query.replace(" ", "+") + "&restrict_sr=1&limit=10&sort=relevance";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "VocationalGuidanceApp/1.0 (Educational Purpose)");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            return extractRedditData(result);

        } catch (Exception e) {
            System.err.println("Error searching subreddit: " + e.getMessage());
            return getDefaultRedditData();
        }
    }

    private Map<String, Object> extractRedditData(Map<String, Object> redditResponse) {
        Map<String, Object> extracted = new HashMap<>();
        List<Map<String, Object>> posts = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) redditResponse.get("data");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) data.get("children");

            int totalUpvotes = 0;
            int totalComments = 0;

            for (Map<String, Object> child : children) {
                @SuppressWarnings("unchecked")
                Map<String, Object> postData = (Map<String, Object>) child.get("data");

                Map<String, Object> post = new HashMap<>();
                post.put("title", postData.get("title"));
                post.put("author", postData.get("author"));
                post.put("score", postData.get("score"));
                post.put("numComments", postData.get("num_comments"));
                post.put("url", "https://reddit.com" + postData.get("permalink"));
                post.put("subreddit", postData.get("subreddit"));
                post.put("created", postData.get("created_utc"));

                String selftext = (String) postData.get("selftext");
                if (selftext != null && !selftext.isEmpty()) {
                    post.put("snippet", selftext.length() > 200 ?
                            selftext.substring(0, 200) + "..." : selftext);
                }

                posts.add(post);

                totalUpvotes += ((Number) postData.get("score")).intValue();
                totalComments += ((Number) postData.get("num_comments")).intValue();
            }

            extracted.put("posts", posts);
            extracted.put("totalPosts", posts.size());
            extracted.put("totalUpvotes", totalUpvotes);
            extracted.put("totalComments", totalComments);
            extracted.put("averageScore", posts.isEmpty() ? 0 : totalUpvotes / posts.size());

        } catch (Exception e) {
            System.err.println("Error extracting Reddit data: " + e.getMessage());
            e.printStackTrace();
        }

        return extracted;
    }

    private String generateCareerSummary(Object generalData, Object careerAdviceData, String careerName) {
        StringBuilder summary = new StringBuilder();
        summary.append("Resumen de información sobre ").append(careerName).append(":\n\n");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> general = (Map<String, Object>) generalData;
            int totalPosts = (int) general.getOrDefault("totalPosts", 0);
            int avgScore = (int) general.getOrDefault("averageScore", 0);

            if (totalPosts > 0) {
                summary.append("- Se encontraron ").append(totalPosts)
                        .append(" discusiones relevantes en Reddit.\n");
                summary.append("- Nivel de interés de la comunidad: ")
                        .append(avgScore > 50 ? "Alto" : avgScore > 20 ? "Moderado" : "Bajo")
                        .append("\n");
                summary.append("- Esta carrera genera conversación activa en comunidades profesionales.\n");
            } else {
                summary.append("- Información limitada en redes sociales. ")
                        .append("Considera consultar fuentes académicas adicionales.\n");
            }

        } catch (Exception e) {
            summary.append("Resumen no disponible temporalmente.");
        }

        return summary.toString();
    }

    private String generateSpecializationSummary(Object discussionData, Object trendsData, String specializationName) {
        StringBuilder summary = new StringBuilder();
        summary.append("Información sobre especialización en ").append(specializationName).append(":\n\n");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> discussion = (Map<String, Object>) discussionData;

            @SuppressWarnings("unchecked")
            Map<String, Object> trends = (Map<String, Object>) trendsData;

            int discussionPosts = (int) discussion.getOrDefault("totalPosts", 0);
            int trendPosts = (int) trends.getOrDefault("totalPosts", 0);

            if (discussionPosts > 0 || trendPosts > 0) {
                summary.append("- Área de especialización con presencia en discusiones profesionales.\n");
                summary.append("- Se encontraron ").append(discussionPosts + trendPosts)
                        .append(" referencias en comunidades especializadas.\n");
            } else {
                summary.append("- Área emergente o altamente especializada con discusión limitada en redes.\n");
            }

        } catch (Exception e) {
            summary.append("Resumen no disponible temporalmente.");
        }

        return summary.toString();
    }

    private Map<String, Object> getDefaultRedditData() {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("posts", new ArrayList<>());
        defaultData.put("totalPosts", 0);
        defaultData.put("totalUpvotes", 0);
        defaultData.put("totalComments", 0);
        defaultData.put("averageScore", 0);
        defaultData.put("message", "No se pudo obtener información de Reddit en este momento");
        return defaultData;
    }
}