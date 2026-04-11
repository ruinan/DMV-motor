package com.dmvmotor.api.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller parameter to be resolved from the Authorization header.
 *
 * Dev-mode: "Authorization: Bearer <userId>" where the token IS the numeric user ID.
 * Production: replace UserIdResolver with real JWT parsing — controllers stay unchanged.
 *
 * Resolves to null for anonymous requests. Controllers that require auth
 * must check for null and throw BusinessException("UNAUTHORIZED", ..., 401).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}
