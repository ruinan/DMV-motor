package com.dmvmotor.api.common;

import com.dmvmotor.api.authaccess.auth.FirebaseAuthVerifier;
import com.dmvmotor.api.authaccess.auth.FirebaseAuthVerifier.VerifiedUser;
import com.dmvmotor.api.authaccess.auth.UserProvisioner;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code @CurrentUser Long userId} parameters from the
 * {@code Authorization: Bearer <token>} header.
 *
 * <p>Pipeline: extract token → {@link FirebaseAuthVerifier} decodes it into a
 * {@code VerifiedUser(firebaseUid, email)} → {@link UserProvisioner} maps the
 * Firebase UID to an internal {@code users.id} (JIT-provisioning on first login).
 *
 * <p>Any missing/invalid/expired token surfaces as {@code null} — controllers
 * that require auth perform the null-check and throw 401 themselves, preserving
 * the pre-Firebase contract.
 */
@Component
public class UserIdResolver implements HandlerMethodArgumentResolver {

    private final FirebaseAuthVerifier verifier;
    private final UserProvisioner provisioner;

    public UserIdResolver(FirebaseAuthVerifier verifier, UserProvisioner provisioner) {
        this.verifier = verifier;
        this.provisioner = provisioner;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        String auth = webRequest.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7).trim();
        try {
            VerifiedUser user = verifier.verify(token);
            return provisioner.provisionUserId(user);
        } catch (BusinessException e) {
            return null;
        }
    }
}
