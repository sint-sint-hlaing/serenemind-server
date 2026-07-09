package com.mental.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final Cloudinary cloudinary;

    /** Cloudinary folder where journal photos are stored. */
    @Value("${cloudinary.journal-photo-folder:serenemind/journal-photos}")
    private String cloudinaryFolder;

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

    // ─────────────────────────────────────────────
    // PHOTO – UPLOAD (Cloudinary)
    // ─────────────────────────────────────────────

    /**
     * POST /api/journals/{id}/photo
     * Upload or replace the photo attached to a journal entry via Cloudinary.
     *
     * Flow:
     *   1. Validate ownership, MIME type, and file size.
     *   2. If an existing photo is stored, destroy it on Cloudinary first.
     *   3. Upload new image to Cloudinary under folder serenemind/journal-photos.
     *   4. Persist the returned secure HTTPS URL on the journal record.
     *
     * Security review:
     *   Ownership validated ✅ | MIME type whitelist (server-side) ✅
     *   File size capped at 5 MB ✅ | Cloudinary public_id uses journal ID (no user input) ✅
     *   Old asset destroyed on Cloudinary before replacement ✅
     *   Credentials in application.properties (never hard-coded) ✅
     *   ⚠️ SECURITY FLAG: Add rate limiting to this endpoint before production.
     *      Risk: Low-Medium — repeated uploads could exhaust Cloudinary free-tier quota.
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
        //    Do NOT rely solely on the client-supplied Content-Type header.
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: JPEG, PNG, WebP, GIF");
        }

        // ── 3. Validate file size (max 5 MB)
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo must not exceed 5 MB");
        }

        try {
            // ── 4. Destroy old Cloudinary asset before replacing
            //    We store the public_id as "folder/journal-{id}" so it is deterministic
            //    and we can always find and destroy it without storing extra metadata.
            String publicId = cloudinaryFolder + "/journal-" + journalId;
            if (journal.getPhotoUrl() != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }

            // ── 5. Upload to Cloudinary
            //    resource_type=image enforces server-side image validation (rejects non-images
            //    even if the MIME header was spoofed).
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",     publicId,
                            "folder",         cloudinaryFolder,
                            "resource_type",  "image",
                            "overwrite",      true,
                            "quality",        "auto",    // auto-compress for performance
                            "fetch_format",   "auto"     // auto-serve WebP/AVIF to supported clients
                    )
            );

            // ── 6. Persist the Cloudinary secure URL (always HTTPS)
            String secureUrl = (String) uploadResult.get("secure_url");
            journal.setPhotoUrl(secureUrl);
            journalRepository.save(journal);

            JournalPhotoResponse response = new JournalPhotoResponse();
            response.setJournalId(journalId);
            response.setPhotoUrl(secureUrl);
            response.setMessage("Photo uploaded successfully");
            return response;

        } catch (IOException e) {
            // Do not expose Cloudinary error details to the client
            throw new RuntimeException("Photo upload failed. Please try again.");
        }
    }

    // ─────────────────────────────────────────────
    // PHOTO – DELETE (Cloudinary)
    // ─────────────────────────────────────────────

    /**
     * DELETE /api/journals/{id}/photo
     * Remove the photo from both Cloudinary and the journal record.
     *
     * Security review:
     *   Ownership validated ✅ | Cloudinary asset destroyed by deterministic public_id ✅
     *   Returns 404 if no photo exists ✅ | No user input reaches Cloudinary API ✅
     */
    @Transactional
    public JournalPhotoResponse deletePhoto(Long journalId, UserPrincipal userPrincipal) {
        Journal journal = findAndValidateOwnership(journalId, userPrincipal);

        if (journal.getPhotoUrl() == null || journal.getPhotoUrl().isBlank()) {
            throw new ResourceNotFoundException("No photo attached to this journal entry");
        }

        try {
            // Destroy on Cloudinary using the deterministic public_id
            String publicId = cloudinaryFolder + "/journal-" + journalId;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // If Cloudinary destroy fails (e.g. already deleted), still clear the DB record
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
     * POST /api/journals/{id}/analysis
     * Trigger (or re-trigger) AI analysis for a journal entry.
     * Currently uses a mock/placeholder analyser; replace with a real AI call.
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

        // ── Mock AI analysis logic ────────────────────────────────────────
        // TODO: Replace this block with a real AI/ML API call (e.g. Gemini,
        //       OpenAI, or a local sentiment model) passing `plainText`.
        analysis.setEmotion(mockDetectEmotion(plainText));
        analysis.setSentiment(mockDetectSentiment(plainText));
        analysis.setStressScore(mockStressScore(plainText));
        analysis.setStressLevel(toStressLevel(analysis.getStressScore()));
        analysis.setKeyThemes(mockKeyThemes(plainText));
        analysis.setAiResponse(mockAiResponse(plainText));
        analysis.setAiSuggestion(mockAiSuggestion(analysis.getEmotion()));
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
            // Strip HTML tags for plain-text preview (prevents XSS in list-card preview)
            String plain = decrypted.replaceAll("<[^>]+>", "").trim();
            response.setPreview(plain.length() > 120 ? plain.substring(0, 120) + "…" : plain);
        }

        response.setTags(stringToTags(journal.getTags()));
        response.setFavourite(journal.isFavourite());
        response.setPrivate(journal.isPrivate());
        response.setPhotoUrl(journal.getPhotoUrl());
        response.setCreatedAt(journal.getCreatedAt());
        response.setUpdatedAt(journal.getUpdatedAt());

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
        r.setAnalysedAt(analysis.getUpdatedAt());
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
