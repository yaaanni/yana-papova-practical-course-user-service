package com.example.UserService.exception;

public class UserWithEmailNotFoundException extends RuntimeException {
    public UserWithEmailNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
