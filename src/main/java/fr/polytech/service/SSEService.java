package fr.polytech.service;

import fr.polytech.model.Notification;
import fr.polytech.model.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSEService {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final Map<UUID, Sinks.Many<NotificationDTO>> userSinks = new ConcurrentHashMap<>();

    public Sinks.Many<NotificationDTO> registerUser(UUID userId) {
        Sinks.Many<NotificationDTO> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.put(userId, sink);
        return sink;
    }

    public void unregisterUser(UUID userId, Sinks.Many<NotificationDTO> sink) {
        sink.tryEmitComplete();
        userSinks.remove(userId);
    }

    public void sendNotificationToOneUser(UUID userId, Notification notification) {
        Sinks.Many<NotificationDTO> sink = userSinks.get(userId);
        if (sink != null) {
            logger.info("Sending notification to user " + userId);
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setId(notification.getId());
            notificationDTO.setDate(notification.getDate());
            notificationDTO.setCategory(notification.getCategory());
            notificationDTO.setMessage(notification.getMessage());
            notificationDTO.setObjectId(notification.getObjectId());

            sink.tryEmitNext(notificationDTO);
        }
    }
}
