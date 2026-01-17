package com.example.UserService.dto.user;

import com.example.UserService.dto.card.CardResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@ToString
public class UserByIdResponse implements Serializable {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDay;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @ToString.Exclude
    private List<CardResponse> cards;
}
