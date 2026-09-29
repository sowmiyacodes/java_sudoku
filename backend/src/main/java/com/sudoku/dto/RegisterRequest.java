package com.sudoku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may contain letters, numbers, dots, underscores, and hyphens")
        String username,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {}