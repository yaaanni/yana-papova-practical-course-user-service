package com.example.UserService.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
