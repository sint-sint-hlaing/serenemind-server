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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private final ChatClient.Builder chatClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_STRESS_SCORE = 100;

    private static final long MAX_PHOTO_SIZE_BYTES =
            5 * 1024 * 1024L;

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "image/gif"
            );

    // ============================================================
    // CREATE JOURNAL
    // ============================================================

    @Transactional
    public JournalResponse createJournal(
            UserPrincipal userPrincipal,
            JournalRequest request
    ) {

        User user = resolveUser(userPrincipal);

        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Journal content must not be empty"
            );
        }

        String encryptedContent =
                encryptionUtil.encrypt(content);

        Journal journal = new Journal();

        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptedContent);
        journal.setUser(user);
        journal.setFavourite(request.isFavourite());
        journal.setTags(tagsToString(request.getTags()));

        Journal saved =
                journalRepository.save(journal);

        return convertToResponse(saved);
    }

    // ============================================================
    // GET ALL JOURNALS
    // ============================================================

    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(
            UserPrincipal userPrincipal,
            String filter
    ) {

        User user = resolveUser(userPrincipal);

        List<Journal> journals;

        if ("favorites".equalsIgnoreCase(filter)) {

            journals =
                    journalRepository
                            .findByUserAndFavouriteTrueOrderByCreatedAtDesc(
                                    user
                            );

        } else if ("tagged".equalsIgnoreCase(filter)) {

            journals =
                    journalRepository.findTaggedByUser(user);

        } else {

            journals =
                    journalRepository
                            .findByUserOrderByCreatedAtDesc(user);
        }

        return journals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(
            UserPrincipal userPrincipal
    ) {

        return getAllMyJournals(
                userPrincipal,
                "all"
        );
    }

    // ============================================================
    // GET JOURNAL BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public JournalResponse getJournalById(
            Long id,
            UserPrincipal userPrincipal
    ) {

        Journal journal =
                findAndValidateOwnership(
                        id,
                        userPrincipal
                );

        return convertToResponse(journal);
    }

    // ============================================================
    // SEARCH JOURNALS
    // ============================================================

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

            results =
                    journalRepository
                            .searchByUserAndTagOnly(
                                    user,
                                    tagQuery
                            );

        } else {

            results =
                    journalRepository
                            .searchByUserAndTitleOrTag(
                                    user,
                                    trimmedQuery
                            );
        }

        return results.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UPDATE JOURNAL
    // ============================================================

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

        String content =
                request.getContent();

        if (
                content == null
                        || content.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Journal content must not be empty"
            );
        }

        journal.setTitle(
                request.getTitle()
        );

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
         * Existing AI analysis is no longer reliable after
         * journal content changes.
         */
        JournalAnalysis existingAnalysis =
                analysisRepository
                        .findByJournal(journal)
                        .orElse(null);

        if (existingAnalysis != null) {

            analysisRepository.delete(
                    existingAnalysis
            );
        }

        Journal saved =
                journalRepository.save(journal);

        return convertToResponse(saved);
    }

    // ============================================================
    // TOGGLE FAVOURITE
    // ============================================================

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

    // ============================================================
    // DELETE JOURNAL
    // ============================================================

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

    // ============================================================
    // UPLOAD PHOTO
    // ============================================================

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

        if (
                file.getSize()
                        > MAX_PHOTO_SIZE_BYTES
        ) {

            throw new IllegalArgumentException(
                    "Photo must not exceed 5 MB"
            );
        }

        /*
         * Delete old photo.
         */
        if (
                journal.getPhotoUrl() != null
                        && !journal
                        .getPhotoUrl()
                        .isBlank()
        ) {

            try {

                cloudinaryService.deleteImage(
                        journal.getPhotoUrl()
                );

            } catch (Exception ignored) {
                // Continue with new upload.
            }
        }

        String secureUrl =
                cloudinaryService.uploadImage(file);

        if (
                secureUrl == null
                        || secureUrl.isBlank()
        ) {

            throw new RuntimeException(
                    "Photo upload failed. Please try again."
            );
        }

        journal.setPhotoUrl(
                secureUrl
        );

        journalRepository.save(journal);

        JournalPhotoResponse response =
                new JournalPhotoResponse();

        response.setJournalId(
                journalId
        );

        response.setPhotoUrl(
                secureUrl
        );

        response.setMessage(
                "Photo uploaded successfully"
        );

        return response;
    }

    // ============================================================
    // DELETE PHOTO
    // ============================================================

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
                        || journal
                        .getPhotoUrl()
                        .isBlank()
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
            // Continue removing database URL.
        }

        journal.setPhotoUrl(null);

        journalRepository.save(journal);

        JournalPhotoResponse response =
                new JournalPhotoResponse();

        response.setJournalId(
                journalId
        );

        response.setPhotoUrl(null);

        response.setMessage(
                "Photo removed successfully"
        );

        return response;
    }

    // ============================================================
    // GET EXISTING AI ANALYSIS
    // ============================================================

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
                                () ->
                                        new ResourceNotFoundException(
                                                "Analysis not found. Trigger it first via the Analyse action."
                                        )
                        );

        return convertAnalysisToResponse(
                analysis
        );
    }

    // ============================================================
    // REAL AI ANALYSIS
    // ============================================================

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

        // --------------------------------------------------------
        // Validate journal content
        // --------------------------------------------------------

        if (
                journal.getEncryptedText() == null
                        || journal
                        .getEncryptedText()
                        .isBlank()
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

        // --------------------------------------------------------
        // Build ChatClient
        // --------------------------------------------------------

        ChatClient chatClient =
                chatClientBuilder.build();

        // --------------------------------------------------------
        // SYSTEM PROMPT
        //
        // IMPORTANT:
        // Journal can be Burmese.
        // AI ANALYSIS MUST ALWAYS BE ENGLISH.
        // --------------------------------------------------------

        String systemPrompt = """
                You are a compassionate and careful mental wellness
                journaling analysis assistant.

                You analyze one private journal entry at a time.

                LANGUAGE REQUIREMENT:
                
                The journal may be written in:
                - Burmese / Myanmar language
                - English
                - Burmese + English
                - Burmese written using English characters
                - Informal Burmese
                - Mixed conversational language

                You MUST understand the journal's meaning regardless
                of its input language.

                HOWEVER, YOUR ENTIRE ANALYSIS OUTPUT MUST ALWAYS BE
                IN ENGLISH.

                This means:

                - emotion MUST be in English.
                - sentiment MUST be in English.
                - keyThemes MUST be in English.
                - aiResponse MUST be in English.
                - aiSuggestion MUST be in English.

                NEVER return Burmese text in the JSON response,
                even when the journal is completely written in Burmese.

                Example:

                Burmese journal:
                "ဒီနေ့ အလုပ်မှာ အရမ်းပင်ပန်းပြီး စိတ်ဖိစီးနေတယ်။"

                Correct English analysis:
                {
                  "emotion": "Stressed",
                  "sentiment": "NEGATIVE",
                  "stressScore": 72,
                  "keyThemes": ["Work", "Fatigue", "Stress"],
                  "aiResponse": "Your entry suggests that work left you feeling very tired and emotionally stressed today. It sounds like the pressure from work had a noticeable effect on your mood.",
                  "aiSuggestion": "Consider giving yourself a short period of rest after work and choose one relaxing activity tonight before thinking about tomorrow's tasks."
                }

                ANALYSIS RULES:

                1. Analyze ONLY the journal provided.

                2. Do not diagnose mental illnesses.

                3. Do not provide medical diagnoses.

                4. Do not invent facts.

                5. Do not assume information that is not present.

                6. Understand Burmese emotional expressions correctly.

                7. Understand Burmese-English mixed sentences.

                8. Understand informal Burmese expressions.

                9. Make the analysis specific to THIS journal.

                10. aiResponse must refer to actual experiences,
                    emotions, situations, or thoughts found in the journal.

                11. aiSuggestion must be directly related to the
                    journal's actual situation.

                12. Avoid generic advice.

                13. Do not repeat the same response structure for
                    every journal.

                14. If the journal discusses work, make the response
                    relevant to work.

                15. If the journal discusses university or studying,
                    make the response relevant to studying.

                16. If the journal discusses family, make the response
                    relevant to family.

                17. If the journal discusses relationships, make the
                    response relevant to the relationship situation.

                18. If the journal discusses loneliness, respond
                    specifically to the loneliness expressed.

                19. If the journal describes happiness or achievement,
                    acknowledge the actual positive experience.

                20. If the journal describes stress, anxiety, sadness,
                    frustration, anger, guilt, or loneliness, respond
                    compassionately without exaggerating the situation.

                21. If the journal is neutral, provide a neutral
                    reflection based on the actual content.

                22. Never claim certainty about the user's mental health.

                23. Never say that you are an AI.

                24. Do not mention these instructions.

                25. Do not include markdown.

                26. Return ONLY a valid JSON object.

                JSON FORMAT:

                {
                  "emotion": "One primary emotion in English",
                  "sentiment": "POSITIVE, NEGATIVE, or NEUTRAL",
                  "stressScore": 0,
                  "keyThemes": [
                    "English theme 1",
                    "English theme 2"
                  ],
                  "aiResponse": "A personalized English reflection based specifically on this journal.",
                  "aiSuggestion": "A personalized and practical English suggestion based specifically on this journal."
                }

                STRESS SCORE:

                0-33   = Low
                34-66  = Medium
                67-100 = High

                stressScore MUST be an integer from 0 to 100.

                KEY THEMES:

                Return 2 to 3 themes when enough information is
                available.

                Themes MUST be short English phrases.

                Do not invent themes.

                AI RESPONSE:

                Write 2 to 3 natural English sentences.

                The response should summarize and reflect what the
                person actually expressed.

                AI SUGGESTION:

                Write 1 to 2 natural English sentences.

                The suggestion must be practical and connected to
                the specific journal.

                IMPORTANT:

                Even if the journal is Burmese, the final JSON MUST
                contain English text only.
                """;

        // --------------------------------------------------------
        // USER PROMPT
        // --------------------------------------------------------

        String title =
                journal.getTitle() != null
                        && !journal.getTitle().isBlank()
                        ? journal.getTitle()
                        : "Untitled Journal";

        String userPrompt = """
                Analyze this journal entry.

                Journal title:
                %s

                Journal content:
                --------------------
                %s
                --------------------

                IMPORTANT OUTPUT REQUIREMENTS:

                1. Understand the journal in its original language.

                2. If the journal is Burmese, understand the Burmese
                   meaning before analyzing it.

                3. If the journal contains Burmese and English,
                   understand both languages together.

                4. Return the analysis ENTIRELY IN ENGLISH.

                5. Do NOT translate the entire journal.

                6. Do NOT repeat the journal.

                7. Make aiResponse specific to this journal.

                8. Make aiSuggestion specific to this journal.

                9. Avoid generic mental wellness statements.

                10. Return ONLY valid JSON.

                Required JSON:

                {
                  "emotion": "English emotion",
                  "sentiment": "POSITIVE, NEGATIVE, or NEUTRAL",
                  "stressScore": 0,
                  "keyThemes": ["English theme 1", "English theme 2"],
                  "aiResponse": "Personalized English reflection.",
                  "aiSuggestion": "Personalized English suggestion."
                }
                """.formatted(
                title,
                plainText
        );

        // --------------------------------------------------------
        // CALL GROQ
        // --------------------------------------------------------

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

            System.out.println(
                    "========== GROQ RAW RESPONSE =========="
            );

            System.out.println(aiContent);

            System.out.println(
                    "========================================"
            );

            // ----------------------------------------------------
            // Clean markdown fences
            //
            // Handles:
            //
            // ```json
            // {...}
            // ```
            //
            // and:
            //
            // ```
            // {...}
            // ```
            // ----------------------------------------------------

            String cleanedJson =
                    cleanJsonResponse(aiContent);

            // ----------------------------------------------------
            // Parse JSON
            // ----------------------------------------------------

            JsonNode resultNode;

            try {

                resultNode =
                        objectMapper.readTree(
                                cleanedJson
                        );

            } catch (Exception jsonException) {

                System.err.println(
                        "Invalid JSON returned by Groq:"
                );

                System.err.println(
                        cleanedJson
                );

                throw new RuntimeException(
                        "Groq returned invalid JSON.",
                        jsonException
                );
            }

            if (
                    resultNode == null
                            || !resultNode.isObject()
            ) {

                throw new RuntimeException(
                        "Groq returned an invalid analysis object."
                );
            }

            // ----------------------------------------------------
            // Validate required fields
            // ----------------------------------------------------

            validateAiResponse(
                    resultNode
            );

            // ----------------------------------------------------
            // Extract emotion
            // ----------------------------------------------------

            String emotion =
                    cleanValue(
                            resultNode
                                    .path("emotion")
                                    .asText()
                    );

            if (emotion.isBlank()) {

                throw new RuntimeException(
                        "AI returned an empty emotion."
                );
            }

            // ----------------------------------------------------
            // Extract sentiment
            // ----------------------------------------------------

            String sentiment =
                    cleanValue(
                            resultNode
                                    .path("sentiment")
                                    .asText()
                    )
                            .toUpperCase();

            if (
                    !sentiment.equals("POSITIVE")
                            && !sentiment.equals("NEGATIVE")
                            && !sentiment.equals("NEUTRAL")
            ) {

                throw new RuntimeException(
                        "AI returned invalid sentiment: "
                                + sentiment
                );
            }

            // ----------------------------------------------------
            // Extract stress score
            // ----------------------------------------------------

            JsonNode stressNode =
                    resultNode.path("stressScore");

            if (
                    !stressNode.isNumber()
                            || stressNode.isFloatingPointNumber()
            ) {

                throw new RuntimeException(
                        "AI returned an invalid stressScore."
                );
            }

            int stressScore =
                    stressNode.asInt();

            if (
                    stressScore < 0
                            || stressScore > MAX_STRESS_SCORE
            ) {

                throw new RuntimeException(
                        "AI returned invalid stressScore: "
                                + stressScore
                );
            }

            // ----------------------------------------------------
            // Extract themes
            // ----------------------------------------------------

            List<String> themes =
                    extractThemes(
                            resultNode.path("keyThemes")
                    );

            if (themes.isEmpty()) {

                throw new RuntimeException(
                        "AI returned no key themes."
                );
            }

            // ----------------------------------------------------
            // Extract AI response
            // ----------------------------------------------------

            String aiResponse =
                    cleanValue(
                            resultNode
                                    .path("aiResponse")
                                    .asText()
                    );

            if (aiResponse.isBlank()) {

                throw new RuntimeException(
                        "AI returned an empty aiResponse."
                );
            }

            // ----------------------------------------------------
            // Extract AI suggestion
            // ----------------------------------------------------

            String aiSuggestion =
                    cleanValue(
                            resultNode
                                    .path("aiSuggestion")
                                    .asText()
                    );

            if (aiSuggestion.isBlank()) {

                throw new RuntimeException(
                        "AI returned an empty aiSuggestion."
                );
            }

            // ----------------------------------------------------
            // Get existing analysis
            // ----------------------------------------------------

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

            // ----------------------------------------------------
            // SAVE REAL AI DATA
            // ----------------------------------------------------

            analysis.setEmotion(
                    emotion
            );

            analysis.setSentiment(
                    sentiment
            );

            analysis.setStressScore(
                    stressScore
            );

            analysis.setStressLevel(
                    toStressLevel(
                            stressScore
                    )
            );

            analysis.setKeyThemes(
                    String.join(
                            ",",
                            themes
                    )
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

        } catch (RuntimeException e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "REAL AI JOURNAL ANALYSIS FAILED"
            );

            System.err.println(
                    "Journal ID: " + journalId
            );

            System.err.println(
                    "Error: " + e.getMessage()
            );

            System.err.println(
                    "========================================"
            );

            throw new RuntimeException(
                    "AI analysis failed. Please try again later.",
                    e
            );

        } catch (Exception e) {

            System.err.println(
                    "Unexpected AI analysis error: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "AI analysis failed. Please try again later.",
                    e
            );
        }
    }

    // ============================================================
    // CLEAN GROQ JSON
    // ============================================================

    private String cleanJsonResponse(
            String response
    ) {

        if (response == null) {
            return "";
        }

        String cleaned =
                response.trim();

        /*
         * Remove ```json ... ```
         */
        if (cleaned.startsWith("```")) {

            int firstNewLine =
                    cleaned.indexOf('\n');

            if (firstNewLine >= 0) {

                cleaned =
                        cleaned.substring(
                                firstNewLine + 1
                        );
            }

            int lastFence =
                    cleaned.lastIndexOf("```");

            if (lastFence >= 0) {

                cleaned =
                        cleaned.substring(
                                0,
                                lastFence
                        );
            }
        }

        cleaned =
                cleaned.trim();

        /*
         * Sometimes models return:
         *
         * Here is the JSON:
         * {...}
         *
         * Find the first object and last object.
         */
        int firstBrace =
                cleaned.indexOf('{');

        int lastBrace =
                cleaned.lastIndexOf('}');

        if (
                firstBrace >= 0
                        && lastBrace > firstBrace
        ) {

            cleaned =
                    cleaned.substring(
                            firstBrace,
                            lastBrace + 1
                    );
        }

        return cleaned.trim();
    }

    // ============================================================
    // VALIDATE AI JSON
    // ============================================================

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

        if (!node.get("emotion").isTextual()) {

            throw new RuntimeException(
                    "AI field 'emotion' must be a string."
            );
        }

        if (!node.get("sentiment").isTextual()) {

            throw new RuntimeException(
                    "AI field 'sentiment' must be a string."
            );
        }

        if (!node.get("keyThemes").isArray()) {

            throw new RuntimeException(
                    "AI field 'keyThemes' must be an array."
            );
        }

        if (!node.get("aiResponse").isTextual()) {

            throw new RuntimeException(
                    "AI field 'aiResponse' must be a string."
            );
        }

        if (!node.get("aiSuggestion").isTextual()) {

            throw new RuntimeException(
                    "AI field 'aiSuggestion' must be a string."
            );
        }
    }

    // ============================================================
    // EXTRACT THEMES
    // ============================================================

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

        themesNode.forEach(
                themeNode -> {

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
                }
        );

        return themes.stream()
                .distinct()
                .limit(3)
                .collect(
                        Collectors.toList()
                );
    }

    // ============================================================
    // CLEAN TEXT
    // ============================================================

    private String cleanValue(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // ============================================================
    // RESOLVE USER
    // ============================================================

    private User resolveUser(
            UserPrincipal principal
    ) {

        if (principal == null) {

            throw new UsernameNotFoundException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByEmail(
                        principal.getEmail()
                )
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User not found"
                                )
                );
    }

    // ============================================================
    // OWNERSHIP CHECK
    // ============================================================

    private Journal findAndValidateOwnership(
            Long id,
            UserPrincipal principal
    ) {

        Journal journal =
                journalRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Journal not found"
                                        )
                        );

        if (
                journal.getUser() == null
                        || principal == null
                        || !journal
                        .getUser()
                        .getEmail()
                        .equals(
                                principal.getEmail()
                        )
        ) {

            throw new AccessDeniedException(
                    "Access denied"
            );
        }

        return journal;
    }

    // ============================================================
    // JOURNAL RESPONSE
    // ============================================================

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
                            ? plain.substring(
                            0,
                            120
                    ) + "…"
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

    // ============================================================
    // ANALYSIS RESPONSE
    // ============================================================

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

    // ============================================================
    // TAGS -> STRING
    // ============================================================

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
                .map(
                        tag ->
                                tag.startsWith("#")
                                        ? tag
                                        .substring(1)
                                        .trim()
                                        : tag
                )
                .filter(
                        tag ->
                                !tag.isEmpty()
                )
                .distinct()
                .collect(
                        Collectors.joining(",")
                );
    }

    // ============================================================
    // STRING -> TAGS
    // ============================================================

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
                        tag ->
                                !tag.isEmpty()
                )
                .collect(
                        Collectors.toList()
                );
    }

    // ============================================================
    // STRESS LEVEL
    // ============================================================

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