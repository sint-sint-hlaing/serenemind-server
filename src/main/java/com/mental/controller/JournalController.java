package com.mental.controller;

import com.mental.dto.JournalRequest;
import com.mental.dto.JournalResponse;
import com.mental.security.UserPrincipal;
import com.mental.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    public ResponseEntity<JournalResponse> createJournal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JournalRequest request) {

        JournalResponse response = journalService.createJournal(userPrincipal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ၁။ GET All Journals
    @GetMapping
    public ResponseEntity<List<JournalResponse>> getAllJournals(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<JournalResponse> responses = journalService.getAllMyJournals(userPrincipal);
        return ResponseEntity.ok(responses);
    }

    // ၂။ GET Journal By ID
    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getJournalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        JournalResponse response = journalService.getJournalById(id, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ၃။ PUT Update Journal By ID
    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        JournalResponse response = journalService.updateJournal(id, request, userPrincipal);
        return ResponseEntity.ok(response);
    }

    // ၄။ DELETE Journal By ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        journalService.deleteJournal(id, userPrincipal);
        return ResponseEntity.noContent().build(); // 204 No Content ပြန်ပေးခြင်း
    }
}
