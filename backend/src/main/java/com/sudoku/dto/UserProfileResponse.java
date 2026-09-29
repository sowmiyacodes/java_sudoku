package com.sudoku.dto;

import com.sudoku.model.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String displayName,
        String email,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getCreatedAt());
    }
}