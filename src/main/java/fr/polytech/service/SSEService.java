package fr.polytech.service;

import fr.polytech.model.Notification;
import fr.polytech.model.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSEService {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);
    private final Map<UUID, Sinks.Many<NotificationDTO>> userSinks = new ConcurrentHashMap<>();

    /**
     * Register a user to the SSE service.
     *
     * @param userId The user id.
     * @return The sink to send notifications to the user.
     */
    public Sinks.Many<NotificationDTO> registerUser(UUID userId) {
        if (!userSinks.containsKey(userId)) {
            logger.info("Registering user {}", userId);
            return userSinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());
        } else {
            logger.info("User {} is already registered, returning existing sink", userId);
            return userSinks.get(userId);
        }
    }

    /**
     * Unregister a user from the SSE service.
     *
     * @param userId The user id.
     */
    public void unregisterUser(UUID userId) {
        Optional.ofNullable(userSinks.remove(userId))
                .ifPresent(Sinks.Many::tryEmitComplete);
    }

    /**
     * Send a notification to the concerned user.
     *
     * @param notification The notification to send.
     */
    public void sendNotificationToOneUser(Notification notification) {
        UUID receiverId = UUID.fromString(notification.getReceiverId());
        Optional.ofNullable(userSinks.get(receiverId)).ifPresent(sink -> {
            logger.info("Sending notification to user {}", receiverId);
            NotificationDTO notificationDTO = new NotificationDTO(notification);
            sink.tryEmitNext(notificationDTO);
        });
    }
}
