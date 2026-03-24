package com.finflow.transaction.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 255, message = "Owner name must be between 2 and 255 characters")
    String ownerName,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    String currency
) {
    public CreateAccountRequest {
        if (currency == null || currency.isBlank()) {
            currency = "CHF";
        }
        currency = currency.toUpperCase();
    }
}
