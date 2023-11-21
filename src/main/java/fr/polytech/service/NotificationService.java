package fr.polytech.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.polytech.model.Notification;
import fr.polytech.model.NotificationDTO;
import fr.polytech.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class NotificationService {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SSEService sseService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, SSEService sseService) {
        this.notificationRepository = notificationRepository;
        this.sseService = sseService;
    }

    /**
     * Save and send the notifications to the concerned user.
     *
     * @param notification The notification to send.
     */
    public void createNotification(NotificationDTO notification) {
        logger.info("Creating notification");

        Notification notificationToSave = new Notification();
        notificationToSave.setId(UUID.randomUUID().toString());
        notificationToSave.setReceiverId(notification.getReceiverId().toString());
        notificationToSave.setObjectId(notification.getObjectId().toString());
        notificationToSave.setDate(notification.getDate());
        notificationToSave.setMessage(notification.getMessage());
        notificationToSave.setCategory(notification.getCategory());

        Mono<Notification> notificationMono = notificationRepository.save(notificationToSave);
        notificationMono.subscribe(notificationToSend -> {
            logger.info("Notification created");
            sseService.sendNotificationToOneUser(notificationToSend);
        }, throwable -> logger.error("Error while creating notification", throwable));
    }

    /**
     * Get all notifications.
     *
     * @return List of all notifications.
     */
    public Flux<Notification> getAllNotifications() {
        logger.info("Getting all notifications");
        return notificationRepository.findAll();
    }

    /**
     * Get notification by id.
     *
     * @param id Notification id.
     * @return Notification with the specified id.
     * @throws WebClientResponseException If notification is not found or unauthorized access.
     */
    public Mono<Notification> getNotificationById(String id) {
        logger.info("Getting notification by id: " + id);
        return notificationRepository.findById(id)
                .switchIfEmpty(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, null)));
    }


    /**
     * Get all notifications by receiver id
     *
     * @param userId Receiver id
     * @return List of notifications
     */
    public Flux<Notification> getAllNotificationsByReceiverId(String userId) {
        logger.info("Getting all notifications by receiver id");
        return notificationRepository
                .findAll()
                .filter(notification -> notification.getReceiverId().equals(userId));
    }

    /**
     * Delete notification by id
     *
     * @param id    Notification id
     * @param token Token
     * @return Void
     */
    public Mono<Void> deleteNotification(String id, String token) {
        logger.info("Removing notification with id {}", id);

        if (token == null || !token.contains(" ")) {
            return Mono.error(new WebClientResponseException(HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(), null, null, null));
        }

        String pureToken = token.split(" ")[1];
        DecodedJWT decodedJWT = JWT.decode(pureToken);

        return getNotificationById(id)
                .flatMap(notification -> {
                    if (!notification.getReceiverId().equals(decodedJWT.getSubject())) {
                        return Mono.error(new WebClientResponseException(HttpStatus.FORBIDDEN.value(),
                                HttpStatus.FORBIDDEN.getReasonPhrase(), null, null, null));
                    }
                    return notificationRepository.deleteById(id);
                });
    }


}
