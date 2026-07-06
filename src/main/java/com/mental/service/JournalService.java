package com.mental.service;

import com.mental.dto.JournalRequest;
import com.mental.dto.JournalResponse;
import com.mental.exception.ResourceNotFoundException;
import com.mental.model.entity.Journal;
import com.mental.model.entity.User;
import com.mental.repository.JournalRepository;
import com.mental.repository.UserRepository;
import com.mental.security.UserPrincipal;
import com.mental.utils.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public JournalResponse createJournal(UserPrincipal userPrincipal, JournalRequest request) {
        // ၁။ လက်ရှိ Login ဝင်ထားသော User ကို ရှာဖွေခြင်း
        User user = userRepository.findByEmail(userPrincipal.getEmail()) // သို့မဟုတ် findByUsername မိမိစနစ်အလိုက်သုံးပါ
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // ၂။ Frontend မှ ရလာသော Rich Text Content ကို Encrypt ပြုလုပ်ခြင်း
        String encryptedContent = encryptionUtil.encrypt(request.getContent());

        // ၃။ Journal Entity ဆောက်ပြီး Data ထည့်သွင်းခြင်း
        Journal journal = new Journal();
        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptedContent);
        journal.setUser(user);

        // ၄။ Database တွင် သိမ်းဆည်းခြင်း
        Journal savedJournal = journalRepository.save(journal);

        // ၅။ Response Return ပြန်ရန် (Response ပြန်လျှင် ချက်ချင်း Decrypt ပြန်လုပ်ပေးရမည်)
        JournalResponse response = new JournalResponse();
        response.setId(savedJournal.getId());
        response.setTitle(savedJournal.getTitle());
        response.setContent(encryptionUtil.decrypt(savedJournal.getEncryptedText())); // Decrypted Rich Text

        return response;
    }

    // ၁။ လက်ရှိ Login ဝင်ထားသော User ၏ Journal အားလုံးကို ယူခြင်း (Get All)
    @Transactional(readOnly = true)
    public List<JournalResponse> getAllMyJournals(UserPrincipal userPrincipal) {
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // User နှင့် သက်ဆိုင်သော Journal များကိုသာ ဆွဲထုတ်ခြင်း (Data Isolation)
        return journalRepository.findByUser(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ၂။ Journal တစ်ခုချင်းစီကို ID ဖြင့် ရှာဖွေခြင်း (Get By ID)
    @Transactional(readOnly = true)
    public JournalResponse getJournalById(Long id, UserPrincipal userPrincipal) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));

        // လုံခြုံရေးအရ အခြားသူ၏ Journal ကို ဝင်ကြည့်မရအောင် ကာကွယ်ခြင်း
        if (!journal.getUser().getEmail().equals(userPrincipal.getEmail())) {
            throw new AccessDeniedException("You do not have permission to view this journal");
        }

        return convertToResponse(journal);
    }

    // ၃။ Journal ကို ပြင်ဆင်ခြင်း (Update Journal)
    @Transactional
    public JournalResponse updateJournal(Long id, JournalRequest request, UserPrincipal userPrincipal) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));

        // လုံခြုံရေးစစ်ဆေးခြင်း
        if (!journal.getUser().getEmail().equals(userPrincipal.getEmail())) {
            throw new AccessDeniedException("You do not have permission to update this journal");
        }

        // စာသားအသစ်ကို ထပ်မံ Encrypt လုပ်ပြီး သိမ်းဆည်းခြင်း
        journal.setTitle(request.getTitle());
        journal.setEncryptedText(encryptionUtil.encrypt(request.getContent()));

        Journal updatedJournal = journalRepository.save(journal);
        return convertToResponse(updatedJournal);
    }

    // ၄။ Journal ကို ဖျက်သိမ်းခြင်း (Delete Journal)
    @Transactional
    public void deleteJournal(Long id, UserPrincipal userPrincipal) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + id));

        // လုံခြုံရေးစစ်ဆေးခြင်း
        if (!journal.getUser().getEmail().equals(userPrincipal.getEmail())) {
            throw new AccessDeniedException("You do not have permission to delete this journal");
        }

        journalRepository.delete(journal);
    }

    // Helper Method: Entity မှ DTO (Response) သို့ ပြောင်းလဲပေးပြီး Decrypt တစ်ခါတည်း လုပ်ဆောင်ခြင်း
    private JournalResponse convertToResponse(Journal journal) {
        JournalResponse response = new JournalResponse();
        response.setId(journal.getId());
        response.setTitle(journal.getTitle());
        response.setContent(encryptionUtil.decrypt(journal.getEncryptedText())); // Decrypt လုပ်ခြင်း
//        response.setCreatedAt(journal.getCreatedAt());
        return response;
    }
}
