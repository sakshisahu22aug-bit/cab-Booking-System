package com.labmentix.cabbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an illegal ride lifecycle state transition or booking operation is encountered.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RideException extends RuntimeException {

    public RideException(String message) {
        super(message);
    }
}
