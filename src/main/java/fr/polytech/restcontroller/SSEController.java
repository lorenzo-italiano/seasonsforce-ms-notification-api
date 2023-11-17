package fr.polytech.restcontroller;

import fr.polytech.annotation.IsSender;
import fr.polytech.model.NotificationDTO;
import fr.polytech.service.NotificationService;
import fr.polytech.service.SSEService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification/sse")
public class SSEController {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SSEController.class);

    private final SSEService sseService;

    @Autowired
    public SSEController(SSEService sseService) {
        this.sseService = sseService;
    }

    @GetMapping("/{userId}")
    @IsSender
    public Flux<ServerSentEvent<NotificationDTO>> streamEvents(@PathVariable UUID userId, @RequestHeader("Authorization") String token) {
        logger.info("Starting to stream events for user with id: {}", userId);
        Sinks.Many<NotificationDTO> sink = sseService.registerUser(userId);
        logger.info("Registered user with id: {} for streaming notifications", userId);

        return sink.asFlux()
                .map(notification -> {
                    logger.debug("Sending notification event for user with id: {}", userId);
                    return ServerSentEvent.<NotificationDTO>builder()
                            .id(UUID.randomUUID().toString())
                            .event("notification-event")
                            .data(notification)
                            .build();
                })
                .doOnCancel(() -> {
                    logger.info("Unregistering user with id: {} from streaming notifications", userId);
                    sseService.unregisterUser(userId, sink);
                });
    }

}
