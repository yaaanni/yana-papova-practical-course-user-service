package com.example.UserService.exception;

public class MoreThanFiveCadsException extends RuntimeException {
    public MoreThanFiveCadsException(Long id) {
        super("User witj id: " + id + " cannot have more than five cards");
    }
}
