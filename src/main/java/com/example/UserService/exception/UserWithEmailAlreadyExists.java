package com.example.UserService.exception;

public class UserWithEmailAlreadyExists extends RuntimeException {
    public UserWithEmailAlreadyExists(String message) {
        super(message);
    }
}
