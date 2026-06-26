package com.erp.pizzeria.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    @Test
    void bloqueaTrasCincoFallosSeguidos() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            service.registrarFallo("ana");
        }
        assertFalse(service.estaBloqueado("ana"), "cuatro fallos aun no bloquean");
        service.registrarFallo("ana");
        assertTrue(service.estaBloqueado("ana"), "el quinto fallo bloquea");
    }

    @Test
    void unLoginExitosoReiniciaElContador() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            service.registrarFallo("ana");
        }
        service.registrarExito("ana");
        service.registrarFallo("ana");
        assertFalse(service.estaBloqueado("ana"), "tras un exito el contador vuelve a cero");
    }

    @Test
    void elBloqueoNoAfectaAOtroUsuario() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.registrarFallo("ana");
        }
        assertTrue(service.estaBloqueado("ana"));
        assertFalse(service.estaBloqueado("luis"), "el bloqueo es por usuario");
    }
}
