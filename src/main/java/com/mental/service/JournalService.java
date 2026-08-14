package com.mental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mental.dto.JournalAnalysisResponse;
import com.mental.dto.JournalPhotoResponse;
import com.mental.dto.JournalRequest;
import com.mental.dto.JournalResponse;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CloudinaryService cloudinaryService;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_STRESS_SCORE = 100;

    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024L;

    private static final java.util.Set<String> ALLOWED_MIME_TYPES =
            java.util.Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "image/gif"
            );

    /**
     * Create a new journal.
     */
    @Transactional
    public JournalResponse createJournal(
            UserPrincipal userPrincipal,
            JournalRequest request
    ) {

        User user = resolveUser(userPrincipal);

        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Journal content must not be empty");
        }

        String encryptedContent = encryptionUtil.encrypt(content);

        Journal journal = new Journal();

        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptedContent);
        journal.setUser(user);
        journal.setFavourite(request.isFavourite());
        journal.setTags(tagsToString(request.getTags()));

        Journal saved = journalRepository.save(journal);

        return convertToResponse(saved);
    }

    /**
     * Get all journals belonging to current user.
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(
            UserPrincipal userPrincipal,
            String filter
    ) {

        User user = resolveUser(userPrincipal);

        List<Journal> journals;

        if ("favorites".equalsIgnoreCase(filter)) {

            journals = journalRepository
                    .findByUserAndFavouriteTrueOrderByCreatedAtDesc(user);

        } else if ("tagged".equalsIgnoreCase(filter)) {

            journals = journalRepository.findTaggedByUser(user);

        } else {

            journals = journalRepository
                    .findByUserOrderByCreatedAtDesc(user);
        }

        return journals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all journals.
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(
            UserPrincipal userPrincipal
    ) {
        return getAllMyJournals(userPrincipal, "all");
    }

    /**
     * Get journal by ID.
     */
    @Transactional(readOnly = true)
    public JournalResponse getJournalById(
            Long id,
            UserPrincipal userPrincipal
    ) {

        Journal journal = findAndValidateOwnership(
                id,
                userPrincipal
        );

        return convertToResponse(journal);
    }

    /**
     * Search journals.
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> searchJournals(
            UserPrincipal userPrincipal,
            String query
    ) {

        User user = resolveUser(userPrincipal);

        String trimmedQuery =
                query != null
                        ? query.trim()
                        : "";

        List<Journal> results;

        if (trimmedQuery.startsWith("#")) {

            String tagQuery =
                    trimmedQuery
                            .substring(1)
                            .trim();

            results = journalRepository
                    .searchByUserAndTagOnly(
                            user,
                            tagQuery
                    );

        } else {

            results = journalRepository
                    .searchByUserAndTitleOrTag(
                            user,
                            trimmedQuery
                    );
        }

        return results.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update journal.
     */
    @Transactional
    public JournalResponse updateJournal(
            Long id,
            JournalRequest request,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        id,
                        userPrincipal
                );

        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Journal content must not be empty"
            );
        }

        journal.setTitle(request.getTitle());

        journal.setEncryptedText(
                encryptionUtil.encrypt(content)
        );

        journal.setTags(
                tagsToString(request.getTags())
        );

        journal.setFavourite(
                request.isFavourite()
        );

        /*
         * If journal content changes, old AI analysis is no longer
         * guaranteed to represent the journal.
         *
         * Delete the old analysis so the user can analyse the
         * updated journal again.
         */
        JournalAnalysis existingAnalysis =
                analysisRepository
                        .findByJournal(journal)
                        .orElse(null);

        if (existingAnalysis != null) {
            analysisRepository.delete(existingAnalysis);
        }

        Journal saved =
                journalRepository.save(journal);

        return convertToResponse(saved);
    }

    /**
     * Toggle favourite.
     */
    @Transactional
    public JournalResponse toggleFavourite(
            Long id,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        id,
                        userPrincipal
                );

        journal.setFavourite(
                !journal.isFavourite()
        );

        return convertToResponse(
                journalRepository.save(journal)
        );
    }

    /**
     * Delete journal.
     */
    @Transactional
    public void deleteJournal(
            Long id,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        id,
                        userPrincipal
                );

        journalRepository.delete(journal);
    }

    /**
     * Upload journal photo.
     */
    @Transactional
    public JournalPhotoResponse uploadPhoto(
            Long journalId,
            MultipartFile file,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        journalId,
                        userPrincipal
                );

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Photo file must not be empty"
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null
                        || !ALLOWED_MIME_TYPES.contains(
                        contentType.toLowerCase()
                )
        ) {

            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: JPEG, PNG, WebP, GIF"
            );
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Photo must not exceed 5 MB"
            );
        }

        /*
         * Delete previous Cloudinary image.
         */
        if (
                journal.getPhotoUrl() != null
                        && !journal.getPhotoUrl().isBlank()
        ) {

            try {

                cloudinaryService.deleteImage(
                        journal.getPhotoUrl()
                );

            } catch (Exception ignored) {
                // Do not stop new upload because old image deletion failed.
            }
        }

        String secureUrl =
                cloudinaryService.uploadImage(file);

        if (secureUrl == null || secureUrl.isBlank()) {
            throw new RuntimeException(
                    "Photo upload failed. Please try again."
            );
        }

        journal.setPhotoUrl(secureUrl);

        journalRepository.save(journal);

        JournalPhotoResponse response =
                new JournalPhotoResponse();

        response.setJournalId(journalId);
        response.setPhotoUrl(secureUrl);
        response.setMessage(
                "Photo uploaded successfully"
        );

        return response;
    }

    /**
     * Delete journal photo.
     */
    @Transactional
    public JournalPhotoResponse deletePhoto(
            Long journalId,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        journalId,
                        userPrincipal
                );

        if (
                journal.getPhotoUrl() == null
                        || journal.getPhotoUrl().isBlank()
        ) {

            throw new ResourceNotFoundException(
                    "No photo attached to this journal entry"
            );
        }

        try {

            cloudinaryService.deleteImage(
                    journal.getPhotoUrl()
            );

        } catch (Exception ignored) {
            // Continue removing URL from database.
        }

        journal.setPhotoUrl(null);

        journalRepository.save(journal);

        JournalPhotoResponse response =
                new JournalPhotoResponse();

        response.setJournalId(journalId);
        response.setPhotoUrl(null);
        response.setMessage(
                "Photo removed successfully"
        );

        return response;
    }

    /**
     * Get existing AI analysis.
     */
    @Transactional(readOnly = true)
    public JournalAnalysisResponse getAnalysis(
            Long journalId,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        journalId,
                        userPrincipal
                );

        JournalAnalysis analysis =
                analysisRepository
                        .findByJournal(journal)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Analysis not found. Trigger it first via the Analyse action."
                                )
                        );

        return convertAnalysisToResponse(
                analysis
        );
    }

    /**
     * Generate REAL AI analysis using Groq.
     *
     * There is intentionally NO mock/fallback analysis.
     */
    @Transactional
    public JournalAnalysisResponse triggerAnalysis(
            Long journalId,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        journalId,
                        userPrincipal
                );

        if (
                journal.getEncryptedText() == null
                        || journal.getEncryptedText().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Cannot analyse an empty journal."
            );
        }

        String plainText;

        try {

            plainText =
                    encryptionUtil.decrypt(
                            journal.getEncryptedText()
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to decrypt journal content for analysis.",
                    e
            );
        }

        if (
                plainText == null
                        || plainText.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Cannot analyse an empty journal."
            );
        }

        /*
         * Build a new ChatClient.
         *
         * Spring AI automatically uses:
         *
         * spring.ai.openai.api-key
         * spring.ai.openai.base-url
         * spring.ai.openai.chat.model
         * spring.ai.openai.chat.temperature
         */
        ChatClient chatClient =
                chatClientBuilder.build();

        String systemPrompt = """
                You are a compassionate and careful mental wellness
                journaling assistant.

                Your task is to analyze ONE journal entry.

                Important rules:

                1. Analyze ONLY the journal entry provided by the user.
                2. Do not diagnose mental illnesses.
                3. Do not make medical diagnoses.
                4. Do not invent facts that are not present in the journal.
                5. The aiResponse must clearly reflect the actual content
                   and emotions expressed in this specific journal.
                6. The aiSuggestion must be specifically relevant to this
                   journal entry.
                7. Avoid generic repeated advice.
                8. Be empathetic, supportive, practical, and concise.
                9. If the journal contains positive experiences, acknowledge
                   those specific experiences.
                10. If the journal contains stress, sadness, anxiety,
                    frustration, loneliness, or another difficult emotion,
                    respond compassionately without exaggerating it.
                11. If the journal is neutral, provide a neutral reflection.
                12. Never claim certainty about the user's mental health.
                13. Do not mention that you are an AI.
                14. Return ONLY valid JSON.

                JSON format:

                {
                  "emotion": "One primary emotion",
                  "sentiment": "POSITIVE, NEGATIVE, or NEUTRAL",
                  "stressScore": 0,
                  "keyThemes": ["Theme 1", "Theme 2"],
                  "aiResponse": "A personalized empathetic reflection in 2-3 sentences.",
                  "aiSuggestion": "A personalized and actionable suggestion in 1-2 sentences."
                }

                stressScore rules:

                0-33   = Low stress
                34-66  = Medium stress
                67-100 = High stress

                The stress score must be an integer between 0 and 100.

                keyThemes must contain 2 to 3 concise themes when enough
                information is available. Do not invent themes.
                """;

        String userPrompt = """
                Analyze the following journal entry.

                Journal title:
                %s

                Journal content:
                ---
                %s
                ---

                Remember:
                - Make aiResponse specific to THIS journal.
                - Make aiSuggestion specific to THIS journal.
                - Do not give generic filler.
                - Return JSON only.
                """.formatted(
                journal.getTitle() != null
                        ? journal.getTitle()
                        : "Untitled",
                plainText
        );

        try {

            String aiContent =
                    chatClient
                            .prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .content();

            if (
                    aiContent == null
                            || aiContent.isBlank()
            ) {

                throw new RuntimeException(
                        "Groq returned an empty AI response."
                );
            }

            /*
             * Parse JSON returned by Groq.
             */
            JsonNode resultNode =
                    objectMapper.readTree(
                            aiContent.trim()
                    );

            if (
                    resultNode == null
                            || !resultNode.isObject()
            ) {

                throw new RuntimeException(
                        "Groq returned an invalid analysis format."
                );
            }

            /*
             * Validate required AI fields.
             */
            validateAiResponse(resultNode);

            /*
             * Extract AI values.
             */
            String emotion =
                    cleanValue(
                            resultNode.path("emotion").asText()
                    );

            String sentiment =
                    cleanValue(
                            resultNode.path("sentiment").asText()
                    );

            int stressScore =
                    resultNode
                            .path("stressScore")
                            .asInt(-1);

            String aiResponse =
                    cleanValue(
                            resultNode
                                    .path("aiResponse")
                                    .asText()
                    );

            String aiSuggestion =
                    cleanValue(
                            resultNode
                                    .path("aiSuggestion")
                                    .asText()
                    );

            /*
             * Validate stress score.
             */
            if (
                    stressScore < 0
                            || stressScore > MAX_STRESS_SCORE
            ) {

                throw new RuntimeException(
                        "AI returned an invalid stress score."
                );
            }

            /*
             * Normalize sentiment.
             */
            sentiment =
                    sentiment.toUpperCase();

            if (
                    !sentiment.equals("POSITIVE")
                            && !sentiment.equals("NEGATIVE")
                            && !sentiment.equals("NEUTRAL")
            ) {

                throw new RuntimeException(
                        "AI returned an invalid sentiment."
                );
            }

            /*
             * Extract themes.
             */
            List<String> themes =
                    extractThemes(
                            resultNode.path("keyThemes")
                    );

            if (themes.isEmpty()) {

                throw new RuntimeException(
                        "AI returned no key themes."
                );
            }

            /*
             * Get existing analysis or create new one.
             */
            JournalAnalysis analysis =
                    analysisRepository
                            .findByJournal(journal)
                            .orElseGet(() -> {

                                JournalAnalysis newAnalysis =
                                        new JournalAnalysis();

                                newAnalysis.setJournal(
                                        journal
                                );

                                return newAnalysis;
                            });

            /*
             * Save REAL AI results.
             */
            analysis.setEmotion(emotion);
            analysis.setSentiment(sentiment);
            analysis.setStressScore(stressScore);
            analysis.setStressLevel(
                    toStressLevel(stressScore)
            );
            analysis.setKeyThemes(
                    String.join(",", themes)
            );
            analysis.setAiResponse(
                    aiResponse
            );
            analysis.setAiSuggestion(
                    aiSuggestion
            );

            JournalAnalysis saved =
                    analysisRepository.save(
                            analysis
                    );

            return convertAnalysisToResponse(
                    saved
            );

        } catch (Exception e) {

            /*
             * IMPORTANT:
             *
             * Do NOT generate fake/mock results.
             *
             * If Groq fails, tell the client the analysis failed.
             */
            System.err.println(
                    "Real AI journal analysis failed: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "AI analysis failed. Please try again later.",
                    e
            );
        }
    }

    /**
     * Validate AI JSON structure.
     */
    private void validateAiResponse(
            JsonNode node
    ) {

        String[] requiredFields = {
                "emotion",
                "sentiment",
                "stressScore",
                "keyThemes",
                "aiResponse",
                "aiSuggestion"
        };

        for (String field : requiredFields) {

            if (
                    !node.has(field)
                            || node.get(field).isNull()
            ) {

                throw new RuntimeException(
                        "AI response is missing required field: "
                                + field
                );
            }
        }
    }

    /**
     * Extract AI-generated themes.
     */
    private List<String> extractThemes(
            JsonNode themesNode
    ) {

        if (
                themesNode == null
                        || !themesNode.isArray()
        ) {
            return Collections.emptyList();
        }

        List<String> themes =
                new ArrayList<>();

        themesNode.forEach(themeNode -> {

            if (
                    themeNode != null
                            && themeNode.isTextual()
            ) {

                String theme =
                        themeNode
                                .asText()
                                .trim();

                if (!theme.isEmpty()) {
                    themes.add(theme);
                }
            }
        });

        /*
         * Remove duplicate themes while preserving order.
         */
        return themes.stream()
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * Clean AI text.
     */
    private String cleanValue(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    /**
     * Resolve authenticated user.
     */
    private User resolveUser(
            UserPrincipal principal
    ) {

        if (principal == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByEmail(principal.getEmail())
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "User not found"
                        )
                );
    }

    /**
     * Find journal and verify ownership.
     */
    private Journal findAndValidateOwnership(
            Long id,
            UserPrincipal principal
    ) {

        Journal journal =
                journalRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Journal not found"
                                )
                        );

        if (
                journal.getUser() == null
                        || principal == null
                        || !journal
                        .getUser()
                        .getEmail()
                        .equals(principal.getEmail())
        ) {

            throw new AccessDeniedException(
                    "Access denied"
            );
        }

        return journal;
    }

    /**
     * Convert Journal entity to response DTO.
     */
    private JournalResponse convertToResponse(
            Journal journal
    ) {

        JournalResponse response =
                new JournalResponse();

        response.setId(
                journal.getId()
        );

        response.setTitle(
                journal.getTitle()
        );

        if (
                journal.getEncryptedText() != null
                        && !journal
                        .getEncryptedText()
                        .isBlank()
        ) {

            String decrypted =
                    encryptionUtil.decrypt(
                            journal.getEncryptedText()
                    );

            response.setContent(
                    decrypted
            );

            String plain =
                    decrypted != null
                            ? decrypted.trim()
                            : "";

            response.setPreview(
                    plain.length() > 120
                            ? plain.substring(0, 120) + "…"
                            : plain
            );
        }

        response.setTags(
                stringToTags(
                        journal.getTags()
                )
        );

        response.setFavourite(
                journal.isFavourite()
        );

        response.setPhotoUrl(
                journal.getPhotoUrl()
        );

        if (journal.getAnalysis() != null) {

            response.setAnalysis(
                    convertAnalysisToResponse(
                            journal.getAnalysis()
                    )
            );
        }

        return response;
    }

    /**
     * Convert JournalAnalysis entity to response.
     */
    private JournalAnalysisResponse convertAnalysisToResponse(
            JournalAnalysis analysis
    ) {

        JournalAnalysisResponse response =
                new JournalAnalysisResponse();

        response.setId(
                analysis.getId()
        );

        response.setEmotion(
                analysis.getEmotion()
        );

        response.setSentiment(
                analysis.getSentiment()
        );

        response.setStressScore(
                analysis.getStressScore()
        );

        response.setStressLevel(
                analysis.getStressLevel()
        );

        response.setKeyThemes(
                stringToTags(
                        analysis.getKeyThemes()
                )
        );

        response.setAiResponse(
                analysis.getAiResponse()
        );

        response.setAiSuggestion(
                analysis.getAiSuggestion()
        );

        return response;
    }

    /**
     * Convert tags list to database string.
     */
    private String tagsToString(
            List<String> tags
    ) {

        if (
                tags == null
                        || tags.isEmpty()
        ) {
            return null;
        }

        return tags.stream()
                .map(String::trim)
                .map(tag ->
                        tag.startsWith("#")
                                ? tag.substring(1).trim()
                                : tag
                )
                .filter(
                        tag -> !tag.isEmpty()
                )
                .distinct()
                .collect(
                        Collectors.joining(",")
                );
    }

    /**
     * Convert database tag string to list.
     */
    private List<String> stringToTags(
            String tags
    ) {

        if (
                tags == null
                        || tags.isBlank()
        ) {

            return Collections.emptyList();
        }

        return Arrays.stream(
                        tags.split(",")
                )
                .map(String::trim)
                .filter(
                        tag -> !tag.isEmpty()
                )
                .collect(
                        Collectors.toList()
                );
    }

    /**
     * Convert numerical stress score to level.
     */
    private String toStressLevel(
            int score
    ) {

        if (score < 34) {
            return "Low";
        }

        if (score < 67) {
            return "Medium";
        }

        return "High";
    }
}