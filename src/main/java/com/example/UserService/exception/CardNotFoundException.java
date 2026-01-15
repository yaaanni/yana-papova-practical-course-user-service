package com.example.UserService.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
