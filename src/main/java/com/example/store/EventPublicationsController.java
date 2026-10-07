package com.example.store;

import java.time.Instant;
import java.util.List;

import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo para el demo: expone la tabla EVENT_PUBLICATION, que es el outbox de Spring Modulith.
 *
 * Cómo funciona el outbox aquí: cuando {@code OrderManagement.place} publica {@code OrderPlaced},
 * Spring Modulith guarda una fila por cada listener interesado, en la MISMA transacción que el
 * pedido (si el pedido se revierte, la fila tampoco se guarda). El listener corre después del
 * commit; si termina bien, la fila se marca completa; si falla, se queda pendiente. Con
 * "spring.modulith.events.republish-outstanding-events-on-restart=true" (ver application.properties),
 * cada pendiente se reintenta automáticamente al reiniciar la app, sin perder el evento.
 */
@RestController
@RequestMapping("/demo/outbox")
class EventPublicationsController {

    private final EventPublicationRegistry registry;

    EventPublicationsController(EventPublicationRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/pendientes")
    List<PendingPublication> pendientes() {
        return registry.findIncompletePublications().stream()
                .map(publication -> new PendingPublication(
                        publication.getEvent().toString(),
                        publication.getTargetIdentifier().getValue(),
                        publication.getPublicationDate()))
                .toList();
    }

    public record PendingPublication(String event, String listener, Instant publishedAt) {}
}
