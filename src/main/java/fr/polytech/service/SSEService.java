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

    public Sinks.Many<NotificationDTO> registerUser(UUID userId) {
        logger.info("Registering user {}", userId);
        return userSinks.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());
    }

    public void unregisterUser(UUID userId) {
        Optional.ofNullable(userSinks.remove(userId))
                .ifPresent(Sinks.Many::tryEmitComplete);
    }

    public void sendNotificationToOneUser(Notification notification) {
        UUID receiverId = UUID.fromString(notification.getReceiverId());
        Optional.ofNullable(userSinks.get(receiverId)).ifPresent(sink -> {
            logger.info("Sending notification to user {}", receiverId);
            NotificationDTO notificationDTO = new NotificationDTO(notification);
            sink.tryEmitNext(notificationDTO);
        });
    }
}
