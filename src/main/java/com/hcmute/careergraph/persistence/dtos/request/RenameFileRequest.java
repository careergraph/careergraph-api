package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameFileRequest(
    @NotBlank(message = "newName is required")
    @Size(min = 1, max = 255, message = "newName must be between 1 and 255 characters")
    String newName
) {}
