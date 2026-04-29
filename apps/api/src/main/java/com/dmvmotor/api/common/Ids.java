package com.dmvmotor.api.common;

import org.springframework.http.HttpStatus;

/**
 * Parses string IDs from request bodies into Long. The wire contract carries IDs as
 * strings (api-contract.md), but they're persisted as bigint internally. Wrapping
 * Long.parseLong here keeps NumberFormatException from leaking out as a 500 — instead
 * the caller gets a uniform 400 INVALID_ID_FORMAT envelope.
 */
public final class Ids {
    private Ids() {}

    public static long parse(String value, String fieldName) {
        if (value == null) {
            throw new BusinessException("INVALID_ID_FORMAT",
                    "Invalid " + fieldName + ": must be numeric",
                    HttpStatus.BAD_REQUEST);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("INVALID_ID_FORMAT",
                    "Invalid " + fieldName + ": must be numeric",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
