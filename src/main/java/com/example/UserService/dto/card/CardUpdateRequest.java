package com.example.UserService.dto.card;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Setter
@Getter
@ToString
public class CardUpdateRequest {
    @Pattern(regexp = "\\d{16}", message = "Card number must contain exactly 16 digits")
    private String number;
    @NotBlank(message = "Holder for card is required")
    private String holder;
    @NotNull(message = "Expiration date is required")
    @Future
    private LocalDate expirationDate;
}