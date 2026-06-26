package com.erp.pizzeria.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Cuenta intentos fallidos y exitosos de login a partir de los eventos de Spring Security
 * para alimentar el bloqueo temporal de {@link LoginAttemptService}.
 */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService attemptService;

    public LoginAttemptListener(LoginAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        attemptService.registrarFallo(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        attemptService.registrarExito(event.getAuthentication().getName());
    }
}
