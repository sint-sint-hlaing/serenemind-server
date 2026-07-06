package com.mental.service;

import com.mental.model.entity.AuditLog;
import com.mental.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void logAction(String username, String action, Long targetId, String details) {
        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .targetId(targetId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}