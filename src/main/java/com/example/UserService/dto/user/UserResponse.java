package com.example.UserService.dto.user;

import com.example.UserService.dto.card.CardResponse;
import com.example.UserService.entities.Card;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ToString
@Setter
@Getter
public class UserResponse {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDay;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
