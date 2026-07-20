package com.mental.exception;

/**
 * Custom exception to be thrown when a user cannot be found in the database.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}