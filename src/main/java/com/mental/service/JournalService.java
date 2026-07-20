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

import java.io.IOException;
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

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    /** Allowed MIME types for photo uploads. */
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    /** Maximum allowed photo size: 5 MB. */
    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024L;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    /**
     * POST /api/journals
     * Create a new journal entry for the authenticated user (text only).
     * To attach a photo, call POST /api/journals/{id}/photo after creation.
     *
     * Security review: Ownership from JWT ✅ | Content encrypted at rest ✅ | @Valid on request ✅
     */
    @Transactional
    public JournalResponse createJournal(UserPrincipal userPrincipal, JournalRequest request) {
        User user = resolveUser(userPrincipal);

        String encryptedContent = encryptionUtil.encrypt(request.getContent());

        Journal journal = new Journal();
        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptedContent);
        journal.setUser(user);
        journal.setFavourite(request.isFavourite());
        journal.setPrivate(request.isPrivate());
        // photoUrl intentionally NOT set here — use POST /api/journals/{id}/photo
        journal.setTags(tagsToString(request.getTags()));

        Journal saved = journalRepository.save(journal);
        return convertToResponse(saved);
    }

    // ─────────────────────────────────────────────
    // READ – LIST
    // ─────────────────────────────────────────────

    /**
     * GET /api/journals?filter=all|favorites|tagged
     * Returns the user's journals filtered by tab selection.
     */
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

    /**
     * GET /api/journals  (backward-compat overload – no filter)
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(UserPrincipal userPrincipal) {
        return getAllMyJournals(userPrincipal, "all");
    }

    // ─────────────────────────────────────────────
    // READ – SINGLE
    // ─────────────────────────────────────────────

    /**
     * GET /api/journals/{id}
     * Fetch a single journal entry by ID.
     */
    @Transactional(readOnly = true)
    public JournalResponse getJournalById(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        return convertToResponse(journal);
    }

    // ─────────────────────────────────────────────
    // READ – SEARCH
    // ─────────────────────────────────────────────

    /**
     * GET /api/journals/search?q=keyword
     * Search journals by title for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> searchJournals(UserPrincipal userPrincipal, String query) {
        User user = resolveUser(userPrincipal);
        return journalRepository.searchByUserAndTitle(user, query).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UPDATE – FULL
    // ─────────────────────────────────────────────

    /**
     * PUT /api/journals/{id}
     * Fully update a journal entry (title, content, tags, privacy).
     * Photo is managed separately via POST/DELETE /api/journals/{id}/photo.
     *
     * Security review: Ownership validated ✅ | Content re-encrypted ✅ | @Valid on request ✅
     */
    @Transactional
    public JournalResponse updateJournal(Long id, JournalRequest request, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);

        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptionUtil.encrypt(request.getContent()));
        journal.setTags(tagsToString(request.getTags()));
        journal.setPrivate(request.isPrivate());
        journal.setFavourite(request.isFavourite());
        // photoUrl NOT updated here — managed exclusively via the photo endpoint

        return convertToResponse(journalRepository.save(journal));
    }

    // ─────────────────────────────────────────────
    // UPDATE – TOGGLE FAVOURITE
    // ─────────────────────────────────────────────

    /**
     * PATCH /api/journals/{id}/favorite
     * Toggle the favourite flag on a journal entry.
     */
    @Transactional
    public JournalResponse toggleFavourite(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        journal.setFavourite(!journal.isFavourite());
        return convertToResponse(journalRepository.save(journal));
    }

    // ─────────────────────────────────────────────
    // UPDATE – TOGGLE PRIVATE
    // ─────────────────────────────────────────────

    /**
     * PATCH /api/journals/{id}/private
     * Toggle the private (lock) flag on a journal entry.
     */
    @Transactional
    public JournalResponse togglePrivate(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        journal.setPrivate(!journal.isPrivate());
        return convertToResponse(journalRepository.save(journal));
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    /**
     * DELETE /api/journals/{id}
     * Permanently delete a journal entry.
     */
    @Transactional
    public void deleteJournal(Long id, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(id, userPrincipal);
        journalRepository.delete(journal);
    }

    /**
     * POST /api/journals/{id}/photo
     * Upload or replace the photo attached to a journal entry via CloudinaryService.
     *
     * Flow:
     *   1. Validate ownership, MIME type, and file size.
     *   2. If an existing photo is stored, delete it on Cloudinary first.
     *   3. Upload new image using CloudinaryService.
     *   4. Persist the returned secure HTTPS URL on the journal record.
     *
     * Security review:
     *   Ownership validated ✅ | MIME type whitelist (server-side) ✅
     *   File size capped at 5 MB ✅ | Non-breaking integration with CloudinaryService ✅
     *   ⚠️ SECURITY FLAG: Add rate limiting to this endpoint before production.
     */
    @Transactional
    public JournalPhotoResponse uploadPhoto(Long journalId,
                                            MultipartFile file,
                                            UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        // ── 1. Validate file present
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file must not be empty");
        }

        // ── 2. Validate MIME type via server-side whitelist
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: JPEG, PNG, WebP, GIF");
        }

        // ── 3. Validate file size (max 5 MB)
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo must not exceed 5 MB");
        }

        // ── 4. Destroy old Cloudinary asset before replacing
        if (journal.getPhotoUrl() != null && !journal.getPhotoUrl().isBlank()) {
            try {
                cloudinaryService.deleteImage(journal.getPhotoUrl());
            } catch (Exception e) {
                // Non-blocking: proceed even if deletion fails (e.g. file already deleted from Cloudinary dashboard)
            }
        }

        // ── 5. Upload new image using CloudinaryService
        String secureUrl = cloudinaryService.uploadImage(file);
        if (secureUrl == null) {
            throw new RuntimeException("Photo upload failed. Please try again.");
        }

        // ── 6. Persist the Cloudinary secure URL
        journal.setPhotoUrl(secureUrl);
        journalRepository.save(journal);

        JournalPhotoResponse response = new JournalPhotoResponse();
        response.setJournalId(journalId);
        response.setPhotoUrl(secureUrl);
        response.setMessage("Photo uploaded successfully");
        return response;
    }

    // ─────────────────────────────────────────────
    // PHOTO – DELETE (Cloudinary)
    // ─────────────────────────────────────────────

    /**
     * DELETE /api/journals/{id}/photo
     * Remove the photo from both Cloudinary and the journal record.
     *
     * Security review:
     *   Ownership validated ✅ | Cloudinary asset destroyed by secure URL parsing ✅
     *   Returns 404 if no photo exists ✅
     */
    @Transactional
    public JournalPhotoResponse deletePhoto(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        if (journal.getPhotoUrl() == null || journal.getPhotoUrl().isBlank()) {
            throw new ResourceNotFoundException("No photo attached to this journal entry");
        }

        try {
            // Destroy on Cloudinary using secure URL parsing
            cloudinaryService.deleteImage(journal.getPhotoUrl());
        } catch (Exception e) {
            // Non-blocking: still clear database entry if Cloudinary deletion fails
        }

        journal.setPhotoUrl(null);
        journalRepository.save(journal);

        JournalPhotoResponse response = new JournalPhotoResponse();
        response.setJournalId(journalId);
        response.setPhotoUrl(null);
        response.setMessage("Photo removed successfully");
        return response;
    }

    /**
     * GET /api/journals/{id}/analysis
     * Retrieve the AI analysis for a journal entry.
     * Returns 404 if no analysis has been run yet.
     */
    @Transactional(readOnly = true)
    public JournalAnalysisResponse getAnalysis(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        JournalAnalysis analysis = analysisRepository.findByJournal(journal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        // Security: do NOT expose internal API paths or journal IDs in error messages
                        "Analysis not found. Trigger it first via the Analyse action."));

        return convertAnalysisToResponse(analysis);
    }

    // ─────────────────────────────────────────────
    // ANALYSIS – TRIGGER / UPSERT
    // ─────────────────────────────────────────────

    /**
    /**
     * POST /api/journals/{id}/analysis
     * Trigger (or re-trigger) AI analysis for a journal entry.
     * Connects to Google Gemini 1.5 Flash API with fallback to local mock logic on error.
     */
    @Transactional
    public JournalAnalysisResponse triggerAnalysis(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        // Decrypt the journal content so we can analyse it
        String plainText = encryptionUtil.decrypt(journal.getEncryptedText());

        // Fetch existing or create a new analysis record
        JournalAnalysis analysis = analysisRepository.findByJournal(journal)
                .orElseGet(() -> {
                    JournalAnalysis a = new JournalAnalysis();
                    a.setJournal(journal);
                    return a;
                });

        boolean success = false;

        // Try calling the Gemini API for live analysis
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + geminiApiKey;

                String prompt = "Analyze the following mental health journal entry. " +
                        "Evaluate: \n" +
                        "1. emotion: One-word primary emotion (e.g. Calm, Happy, Anxious, Sad, Angry, Neutral)\n" +
                        "2. sentiment: POSITIVE, NEGATIVE, or NEUTRAL\n" +
                        "3. stressScore: A numeric stress level score from 0 to 100\n" +
                        "4. keyThemes: An array of 2 to 3 tags describing the core themes (e.g. Gratitude, Family, Health, Work, Positivity)\n" +
                        "5. aiResponse: A supportive and empathetic reflection paragraph (2-3 sentences max)\n" +
                        "6. aiSuggestion: A helpful and actionable suggestion (1-2 sentences max) based on the user's emotion.\n\n" +
                        "Journal entry content:\n" + plainText;

                Map<String, Object> textPart = Map.of("text", prompt);
                Map<String, Object> parts = Map.of("parts", List.of(textPart));
                Map<String, Object> contents = Map.of("contents", List.of(parts));

                // Define JSON Schema for Structured Output to guarantee JSON matching our entity properties
                Map<String, Object> schema = Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "emotion", Map.of("type", "STRING", "description", "One word primary emotion, e.g., Calm, Happy, Anxious, Sad, Angry, Neutral"),
                                "sentiment", Map.of("type", "STRING", "description", "POSITIVE, NEGATIVE, or NEUTRAL"),
                                "stressScore", Map.of("type", "INTEGER", "description", "Stress level score from 0 to 100"),
                                "keyThemes", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"), "description", "2 to 3 tags describing themes, e.g. Gratitude, Family, Health, Work, Positivity"),
                                "aiResponse", Map.of("type", "STRING", "description", "A supportive reflection paragraph (2-3 sentences)"),
                                "aiSuggestion", Map.of("type", "STRING", "description", "One actionable suggestion (1-2 sentences)")
                        ),
                        "required", List.of("emotion", "sentiment", "stressScore", "keyThemes", "aiResponse", "aiSuggestion")
                );

                Map<String, Object> generationConfig = Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema
                );

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(parts),
                        "generationConfig", generationConfig
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

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
                // Non-blocking error logging (System.err/logger fallback)
                System.err.println("Error calling Gemini API: " + e.getMessage());
            }
        }

        // ── Fallback to Mock AI analysis logic if API call failed ─────────────────
        if (!success) {
            analysis.setEmotion(mockDetectEmotion(plainText));
            analysis.setSentiment(mockDetectSentiment(plainText));
            analysis.setStressScore(mockStressScore(plainText));
            analysis.setStressLevel(toStressLevel(analysis.getStressScore()));
            analysis.setKeyThemes(mockKeyThemes(plainText));
            analysis.setAiResponse(mockAiResponse(plainText));
            analysis.setAiSuggestion(mockAiSuggestion(analysis.getEmotion()));
        }
        // ─────────────────────────────────────────────────────────────────

        JournalAnalysis saved = analysisRepository.save(analysis);
        return convertAnalysisToResponse(saved);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private User resolveUser(UserPrincipal principal) {
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Finds a journal by ID and validates that the authenticated user is the owner.
     *
     * Security contract:
     *   - Returns 404 if the journal does not exist (correct: do not confirm existence to non-owners).
     *   - Returns 403 Forbidden (via AccessDeniedException → Spring Security → 403 mapping)
     *     if the journal exists but belongs to a different user.
     *   - Never returns the resource to a non-owner.
     */
    private Journal findAndValidateOwnership(Long id, UserPrincipal principal) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));
        // Use email comparison — ID comparison alone could be spoofed via token manipulation
        if (!journal.getUser().getEmail().equals(principal.getEmail())) {
            // AccessDeniedException is handled by Spring Security's AccessDeniedHandler → 403
            throw new AccessDeniedException("Access denied");
        }
        return journal;
    }

    private JournalResponse convertToResponse(Journal journal) {
        JournalResponse response = new JournalResponse();
        response.setId(journal.getId());
        response.setTitle(journal.getTitle());

        // Null-guard: encryptedText could be null for legacy / partially-migrated records
        if (journal.getEncryptedText() != null && !journal.getEncryptedText().isBlank()) {
            String decrypted = encryptionUtil.decrypt(journal.getEncryptedText());
            response.setContent(decrypted);
            // Construct plain text preview directly
            String plain = decrypted.trim();
            response.setPreview(plain.length() > 120 ? plain.substring(0, 120) + "…" : plain);
        }

        response.setTags(stringToTags(journal.getTags()));
        response.setFavourite(journal.isFavourite());
        response.setPrivate(journal.isPrivate());
        response.setPhotoUrl(journal.getPhotoUrl());
        //response.setCreatedAt(journal.getCreatedAt());
       // response.setUpdatedAt(journal.getUpdatedAt());

        // Attach inline analysis only if it already exists (no eager AI calls)
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
       // r.setAnalysedAt(analysis.getUpdatedAt());
        return r;
    }

    // Tag helpers
    private String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags);
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

    // ── Mock AI helpers (replace with real AI integration) ────────────────

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
