package fr.polytech.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.polytech.model.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaService(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen to the topic "notification-topic" and send the notification to the concerned user.
     *
     * @param message The message received from the topic.
     */
    @KafkaListener(topics = "offer-topic", groupId = "notification")
    public void listenOffer(String message) {
        try {
            NotificationDTO notification = messageToNotificationList(message);
            notificationService.createNotification(notification);
        } catch (JsonProcessingException e) {
            logger.error("Error while parsing message", e);
        }
    }

    /**
     * Parse the message received from the topic.
     *
     * @param message The message received from the topic.
     * @return The notification to send to the concerned user.
     * @throws JsonProcessingException If the message cannot be parsed.
     */
    private NotificationDTO messageToNotificationList(String message) throws JsonProcessingException {
        NotificationDTO notification = objectMapper.readValue(message, new TypeReference<NotificationDTO>() {
        });
        logger.info("Notification received: {}", notification);
        return notification;
    }
}
