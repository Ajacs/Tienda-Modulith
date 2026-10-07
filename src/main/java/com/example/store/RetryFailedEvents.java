package com.example.store;

import java.time.Duration;

import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los eventos del outbox que quedaron pendientes: cada 30 segundos, esperando al menos
 * 30 segundos desde el último intento y dejando de insistir al quinto fallo.
 */
@Component
class RetryFailedEvents {

    private final FailedEventPublications failed;

    RetryFailedEvents(FailedEventPublications failed) {
        this.failed = failed;
    }

    @Scheduled(fixedDelay = 30_000)
    void retry() {
        failed.resubmit(ResubmissionOptions.defaults()
                .withMinAge(Duration.ofSeconds(30))
                .withFilter(p -> p.getCompletionAttempts() < 5));
    }
}
