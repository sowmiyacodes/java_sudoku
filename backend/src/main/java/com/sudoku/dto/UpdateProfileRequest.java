package com.sudoku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 254) String email
) {}