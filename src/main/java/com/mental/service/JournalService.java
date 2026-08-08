package com.mental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mental.dto.*;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Journal;
import com.mental.model.entity.JournalAnalysis;
import com.mental.model.entity.User;
import com.mental.repository.JournalAnalysisRepository;
import com.mental.repository.JournalRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import com.mental.utils.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CloudinaryService cloudinaryService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024L;

    @Transactional
    public JournalResponse createJournal(UserPrincipal userPrincipal, JournalRequest request) {
        User user = resolveUser(userPrincipal);

        String encryptedContent = encryptionUtil.encrypt(request.getContent());

        Journal journal = new Journal();
        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptedContent);
        journal.setUser(user);
        journal.setFavourite(request.isFavourite());
        journal.setTags(tagsToString(request.getTags()));

        Journal saved = journalRepository.save(journal);
        return convertToResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(UserPrincipal userPrincipal, String filter) {
        User user = resolveUser(userPrincipal);

        List<Journal> journals;
        if ("favorites".equalsIgnoreCase(filter)) {
            journals = journalRepository.findByUserAndFavouriteTrueOrderByCreatedAtDesc(user);
        } else if ("tagged".equalsIgnoreCase(filter)) {
            journals = journalRepository.findTaggedByUser(user);
        } else {
            journals = journalRepository.findByUserOrderByCreatedAtDesc(user);
        }

        return journals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(UserPrincipal userPrincipal) {
        return getAllMyJournals(userPrincipal, "all");
    }


    @Transactional(readOnly = true)
    public JournalResponse getJournalById(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        return convertToResponse(journal);
    }


    @Transactional(readOnly = true)
    public List<JournalResponse> searchJournals(UserPrincipal userPrincipal, String query) {
        User user = resolveUser(userPrincipal);
        String trimmedQuery = query != null ? query.trim() : "";

        List<Journal> results;
        if (trimmedQuery.startsWith("#")) {
            String tagQuery = trimmedQuery.substring(1).trim();
            results = journalRepository.searchByUserAndTagOnly(user, tagQuery);
        } else {
            results = journalRepository.searchByUserAndTitleOrTag(user, trimmedQuery);
        }

        return results.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public JournalResponse updateJournal(Long id, JournalRequest request, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);

        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptionUtil.encrypt(request.getContent()));
        journal.setTags(tagsToString(request.getTags()));
        journal.setFavourite(request.isFavourite());

        return convertToResponse(journalRepository.save(journal));
    }


    @Transactional
    public JournalResponse toggleFavourite(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        journal.setFavourite(!journal.isFavourite());
        return convertToResponse(journalRepository.save(journal));
    }


    @Transactional
    public void deleteJournal(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        journalRepository.delete(journal);
    }


    @Transactional
    public JournalPhotoResponse uploadPhoto(Long journalId,
                                            MultipartFile file,
                                            UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: JPEG, PNG, WebP, GIF");
        }


        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo must not exceed 5 MB");
        }

        if (journal.getPhotoUrl() != null && !journal.getPhotoUrl().isBlank()) {
            try {
                cloudinaryService.deleteImage(journal.getPhotoUrl());
            } catch (Exception e) {
           }
        }

       String secureUrl = cloudinaryService.uploadImage(file);
        if (secureUrl == null) {
            throw new RuntimeException("Photo upload failed. Please try again.");
        }

        journal.setPhotoUrl(secureUrl);
        journalRepository.save(journal);

        JournalPhotoResponse response = new JournalPhotoResponse();
        response.setJournalId(journalId);
        response.setPhotoUrl(secureUrl);
        response.setMessage("Photo uploaded successfully");
        return response;
    }


    @Transactional
    public JournalPhotoResponse deletePhoto(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        if (journal.getPhotoUrl() == null || journal.getPhotoUrl().isBlank()) {
            throw new ResourceNotFoundException("No photo attached to this journal entry");
        }

        try {
            cloudinaryService.deleteImage(journal.getPhotoUrl());
        } catch (Exception e) {
        }

        journal.setPhotoUrl(null);
        journalRepository.save(journal);

        JournalPhotoResponse response = new JournalPhotoResponse();
        response.setJournalId(journalId);
        response.setPhotoUrl(null);
        response.setMessage("Photo removed successfully");
        return response;
    }


    @Transactional(readOnly = true)
    public JournalAnalysisResponse getAnalysis(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        JournalAnalysis analysis = analysisRepository.findByJournal(journal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found. Trigger it first via the Analyse action."));

        return convertAnalysisToResponse(analysis);
    }

    @Transactional
    public JournalAnalysisResponse triggerAnalysis(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        String plainText = encryptionUtil.decrypt(journal.getEncryptedText());


        JournalAnalysis analysis = analysisRepository.findByJournal(journal)
                .orElseGet(() -> {
                    JournalAnalysis a = new JournalAnalysis();
                    a.setJournal(journal);
                    return a;
                });

        boolean success = false;


        if (groqApiKey != null && !groqApiKey.isBlank()) {
            try {
                String url = "https://api.groq.com/openai/v1/chat/completions";

                String prompt = "Analyze the following mental health journal entry.\n\n" +
                        "Respond ONLY with a raw JSON object containing exactly these keys:\n" +
                        "- emotion: One-word primary emotion (e.g. Calm, Happy, Anxious, Sad, Angry, Neutral)\n" +
                        "- sentiment: POSITIVE, NEGATIVE, or NEUTRAL\n" +
                        "- stressScore: A numeric stress level score from 0 to 100\n" +
                        "- keyThemes: An array of 2 to 3 strings describing the core themes (e.g. [\"Work\", \"Stress\", \"Family\"])\n" +
                        "- aiResponse: A supportive and empathetic reflection paragraph (2-3 sentences max)\n" +
                        "- aiSuggestion: A helpful and actionable suggestion (1-2 sentences max)\n\n" +
                        "Journal entry content:\n" + plainText;

                Map<String, Object> systemMessage = Map.of(
                        "role", "system",
                        "content", "You are a compassionate mental health AI assistant. Always respond strictly with valid JSON only — no markdown, no extra text."
                );
                Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

                Map<String, Object> requestBody = Map.of(
                        "model", "llama-3.3-70b-versatile",
                        "messages", List.of(systemMessage, userMessage),
                        "response_format", Map.of("type", "json_object")
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String jsonText = root.path("choices").get(0).path("message").path("content").asText();

                    JsonNode resultNode = objectMapper.readTree(jsonText);

                    analysis.setEmotion(resultNode.path("emotion").asText("Neutral"));
                    analysis.setSentiment(resultNode.path("sentiment").asText("NEUTRAL"));
                    analysis.setStressScore(resultNode.path("stressScore").asInt(30));
                    analysis.setStressLevel(toStressLevel(analysis.getStressScore()));

                    List<String> themes = new java.util.ArrayList<>();
                    resultNode.path("keyThemes").forEach(t -> themes.add(t.asText()));
                    analysis.setKeyThemes(String.join(",", themes));

                    analysis.setAiResponse(resultNode.path("aiResponse").asText("Your entry reflects a thoughtful processing of your thoughts. Keep journaling as a healthy habit."));
                    analysis.setAiSuggestion(resultNode.path("aiSuggestion").asText("Consider doing a breathing exercise to rest your mind."));

                    success = true;
                }
            } catch (Exception e) {
                System.err.println("Error calling Groq API: " + e.getMessage());
            }
        }

        if (!success) {
            analysis.setEmotion(mockDetectEmotion(plainText));
            analysis.setSentiment(mockDetectSentiment(plainText));
            analysis.setStressScore(mockStressScore(plainText));
            analysis.setStressLevel(toStressLevel(analysis.getStressScore()));
            analysis.setKeyThemes(mockKeyThemes(plainText));
            analysis.setAiResponse(mockAiResponse(plainText));
            analysis.setAiSuggestion(mockAiSuggestion(analysis.getEmotion()));
        }

        JournalAnalysis saved = analysisRepository.save(analysis);
        return convertAnalysisToResponse(saved);
    }


    private User resolveUser(UserPrincipal principal) {
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Journal findAndValidateOwnership(Long id, UserPrincipal principal) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));
        if (!journal.getUser().getEmail().equals(principal.getEmail())) {
            throw new AccessDeniedException("Access denied");
        }
        return journal;
    }

    private JournalResponse convertToResponse(Journal journal) {
        JournalResponse response = new JournalResponse();
        response.setId(journal.getId());
        response.setTitle(journal.getTitle());

        if (journal.getEncryptedText() != null && !journal.getEncryptedText().isBlank()) {
            String decrypted = encryptionUtil.decrypt(journal.getEncryptedText());
            response.setContent(decrypted);
            String plain = decrypted.trim();
            response.setPreview(plain.length() > 120 ? plain.substring(0, 120) + "…" : plain);
        }

        response.setTags(stringToTags(journal.getTags()));
        response.setFavourite(journal.isFavourite());
        response.setPhotoUrl(journal.getPhotoUrl());
        if (journal.getAnalysis() != null) {
            response.setAnalysis(convertAnalysisToResponse(journal.getAnalysis()));
        }

        return response;
    }

    private JournalAnalysisResponse convertAnalysisToResponse(JournalAnalysis analysis) {
        JournalAnalysisResponse r = new JournalAnalysisResponse();
        r.setId(analysis.getId());
        r.setEmotion(analysis.getEmotion());
        r.setSentiment(analysis.getSentiment());
        r.setStressScore(analysis.getStressScore());
        r.setStressLevel(analysis.getStressLevel());
        r.setKeyThemes(stringToTags(analysis.getKeyThemes()));
        r.setAiResponse(analysis.getAiResponse());
        r.setAiSuggestion(analysis.getAiSuggestion());
        return r;
    }


    private String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(String::trim)
                .map(tag -> tag.startsWith("#") ? tag.substring(1).trim() : tag)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> stringToTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.asList(tags.split(","));
    }

    private String toStressLevel(int score) {
        if (score < 34) return "Low";
        if (score < 67) return "Medium";
        return "High";
    }


    private String mockDetectEmotion(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("happy") || lower.contains("great") || lower.contains("joy")) return "Happy";
        if (lower.contains("calm") || lower.contains("peace") || lower.contains("relax")) return "Calm";
        if (lower.contains("anxious") || lower.contains("worry") || lower.contains("stress")) return "Anxious";
        if (lower.contains("sad") || lower.contains("cry") || lower.contains("miss")) return "Sad";
        return "Neutral";
    }

    private String mockDetectSentiment(String text) {
        String lower = text.toLowerCase();
        long positiveCount = List.of("good", "great", "happy", "love", "wonderful", "amazing", "calm", "peace")
                .stream().filter(lower::contains).count();
        long negativeCount = List.of("bad", "sad", "angry", "hate", "stress", "anxious", "worry", "terrible")
                .stream().filter(lower::contains).count();
        if (positiveCount > negativeCount) return "POSITIVE";
        if (negativeCount > positiveCount) return "NEGATIVE";
        return "NEUTRAL";
    }

    private int mockStressScore(String text) {
        String lower = text.toLowerCase();
        int score = 30; // baseline
        if (lower.contains("stress") || lower.contains("overwhelm")) score += 25;
        if (lower.contains("anxious") || lower.contains("worry")) score += 20;
        if (lower.contains("calm") || lower.contains("peace") || lower.contains("relax")) score -= 15;
        if (lower.contains("happy") || lower.contains("good")) score -= 10;
        return Math.max(0, Math.min(100, score));
    }

    private String mockKeyThemes(String text) {
        String lower = text.toLowerCase();
        List<String> themes = new java.util.ArrayList<>();
        if (lower.contains("family") || lower.contains("parent") || lower.contains("child")) themes.add("Family");
        if (lower.contains("grateful") || lower.contains("gratitude") || lower.contains("thankful")) themes.add("Gratitude");
        if (lower.contains("work") || lower.contains("task") || lower.contains("goal")) themes.add("Productivity");
        if (lower.contains("health") || lower.contains("exercise") || lower.contains("walk")) themes.add("Health");
        if (lower.contains("positive") || lower.contains("hope") || lower.contains("better")) themes.add("Positivity");
        if (themes.isEmpty()) themes.add("Reflection");
        return String.join(",", themes);
    }

    private String mockAiResponse(String text) {
        return "Your journal entry reflects a thoughtful and self-aware mindset. " +
               "The emotions you've expressed suggest you are actively processing your daily experiences, " +
               "which is a healthy and productive practice. Keep acknowledging both the challenges and " +
               "the positive moments in your life.";
    }

    private String mockAiSuggestion(String emotion) {
        return switch (emotion) {
            case "Anxious" -> "You seem a bit stressed. Try to keep this positive momentum going. " +
                              "Consider a short meditation tonight to improve your sleep.";
            case "Sad"     -> "It's okay to feel this way. Reach out to someone you trust today " +
                              "and try a short breathing exercise to lift your mood.";
            case "Happy"   -> "Great day! Consider channelling this energy into a goal you've been " +
                              "putting off. Momentum is your friend right now.";
            default        -> "You're in a good space! Try to keep this positive momentum going. " +
                              "Consider a short meditation tonight to improve your sleep.";
        };
    }


}
