package fr.polytech.restcontroller;

import fr.polytech.annotation.IsSender;
import fr.polytech.model.NotificationDTO;
import fr.polytech.service.SSEService;
import jakarta.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/notification/sse")
public class SSEController {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SSEController.class);

    /**
     * Map a unique token to a user id.
     */
    private final Map<String, UUID> uniqueUrlToUserIdMap = new ConcurrentHashMap<>();

    private final SSEService sseService;

    @Autowired
    public SSEController(SSEService sseService) {
        this.sseService = sseService;
    }

    /**
     * Create a unique token for the user and return it.
     *
     * @param userId The user id.
     * @param token  The JWT token.
     * @return The unique token.
     */
    @PostMapping("/proxy/{userId}")
    @Produces(MediaType.TEXT_PLAIN_VALUE)
    @IsSender
    public ResponseEntity<String> proxy(@PathVariable UUID userId, @RequestHeader("Authorization") String token) {
        String uniqueToken = generateUniqueToken(userId);
        uniqueUrlToUserIdMap.put(uniqueToken, userId);
        return ResponseEntity.ok(uniqueToken);
    }

    /**
     * Generate a unique token for the user. This token is a hash of the user id and a random string.
     * This token is used to identify the user in the SSE stream.
     *
     * @param userId The user id.
     * @return The unique token.
     */
    private String generateUniqueToken(UUID userId) {
        String toHash = userId.toString() + "-" + UUID.randomUUID();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(toHash.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // If the algorithm does not exist, we return the string as is.
            return toHash;
        }
    }

    /**
     * Stream events to the user.
     *
     * @param uniqueToken The unique token of the user.
     * @return A flux of server sent events.
     */
    @GetMapping("/subscribe/{uniqueToken}")
    @Produces(MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDTO>> streamEvents(@PathVariable String uniqueToken) {
        UUID userId = uniqueUrlToUserIdMap.get(uniqueToken);
        if (userId == null) {
            return Flux.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, null));
        }

        Sinks.Many<NotificationDTO> sink = sseService.registerUser(userId);
        return createSseFlux(sink, userId, uniqueToken);
    }

    /**
     * Create a flux of server sent events from a sink.
     *
     * @param sink        The sink.
     * @param userId      The user id.
     * @param uniqueToken The unique token.
     * @return A flux of server sent events.
     */
    private Flux<ServerSentEvent<NotificationDTO>> createSseFlux(Sinks.Many<NotificationDTO> sink, UUID userId, String uniqueToken) {
        return sink.asFlux()
                .map(notification -> ServerSentEvent.builder(notification).build())
                .doOnCancel(() -> {
                    sseService.unregisterUser(userId);
                    uniqueUrlToUserIdMap.remove(uniqueToken);
                })
                .doOnTerminate(() -> {
                    logger.info("SSE stream terminated for user {}", userId);
                    uniqueUrlToUserIdMap.remove(uniqueToken);
                });
    }
}
