package com.sudoku.controller;

import com.sudoku.model.AuditLog;
import com.sudoku.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getLogs(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action) {
        if (entity != null && !entity.isBlank()) {
            return ResponseEntity.ok(auditLogService.getLogsByEntity(entity));
        }
        if (action != null && !action.isBlank()) {
            return ResponseEntity.ok(auditLogService.getLogsByAction(action));
        }
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    @PostMapping
    public ResponseEntity<AuditLog> createLog(@RequestBody Map<String, String> payload) {
        String adminUsername = payload.getOrDefault("adminUsername", "admin");
        String action = payload.getOrDefault("action", "SYSTEM_ACTION");
        String entity = payload.getOrDefault("entity", "GENERAL");
        String entityId = payload.get("entityId");
        String description = payload.getOrDefault("description", "Admin operation performed");

        AuditLog saved = auditLogService.logAction(adminUsername, action, entity, entityId, description);
        return ResponseEntity.ok(saved);
    }
}
