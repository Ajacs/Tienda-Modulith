package com.example.store.notifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simula el envío de correo. Los destinatarios que terminan en "@falla.test" provocan un error,
 * para mostrar qué pasa con el evento cuando un listener falla.
 */
@Component
class ConfirmationMailer {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationMailer.class);

    private final List<String> recipients = new CopyOnWriteArrayList<>();

    void send(String to, String message) {

        if (to.endsWith("@falla.test")) {
            throw new IllegalStateException("Servidor de correo no disponible (simulado)");
        }

        log.info("Correo enviado a {}: {}", to, message);
        recipients.add(to);
    }

    boolean wasSentTo(String to) {
        return recipients.contains(to);
    }
}
