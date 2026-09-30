package com.sudoku.service;

import com.sudoku.model.AuditLog;
import com.sudoku.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditLog logAction(String adminUsername, String action, String entity, String entityId, String description) {
        AuditLog log = new AuditLog(adminUsername, action, entity, entityId, description);
        return repository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return repository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByEntity(String entity) {
        return repository.findByEntityOrderByTimestampDesc(entity);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return repository.findByActionOrderByTimestampDesc(action);
    }
}
