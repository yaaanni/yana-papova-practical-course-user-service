package com.example.UserService.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@ToString
@Getter
@Setter
public class UserRequest {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Surname is required")
    private String surname;
    @NotNull(message = "Birth Date is required")
    @Past(message = "Birthday must be in the past")
    private LocalDate birthDate;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
