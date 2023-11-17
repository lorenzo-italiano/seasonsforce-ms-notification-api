package fr.polytech.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.polytech.model.Notification;
import fr.polytech.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Flow;

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
     * Send the notifications to the concerned user.
     *
     * @param notificationList The notifications to send.
     */
    public void createNotifications(List<Notification> notificationList) {
        notificationList.forEach(notification -> {
            Notification createdNotification = saveNotification(notification);
            sseService.sendNotificationToOneUser(notification.getReceiverId(), createdNotification);
        });
    }

    /**
     * Save the notification in the database.
     *
     * @param notification The notification to save.
     * @return The notification saved.
     */
    private Notification saveNotification(Notification notification) {
        logger.info("Creating notification");
        Mono<Notification> notificationMono = notificationRepository.save(notification);
        return notificationMono.block();
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
     * @throws HttpClientErrorException If notification is not found.
     */
    public Mono<Notification> getNotificationById(String id, String token) throws HttpClientErrorException {
        logger.info("Getting notification by id: " + id);

        String pureToken = token.split(" ")[1];
        if (pureToken == null) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }

        DecodedJWT decodedJWT = JWT.decode(pureToken);
        if (!decodedJWT.getSubject().equals(id)) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }

        return notificationRepository.findById(UUID.fromString(id))
                .switchIfEmpty(Mono.error(new HttpClientErrorException(HttpStatus.NOT_FOUND)));
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
                .filter(notification -> notification.getReceiverId().equals(UUID.fromString(userId)));
    }

    public Mono<Void> deleteNotification(String id, String token) {
        logger.info("Removing notification with id {}", id);

        String pureToken = token.split(" ")[1];
        if (pureToken == null) {
            return Mono.error(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        }

        DecodedJWT decodedJWT = JWT.decode(pureToken);
        if (!decodedJWT.getSubject().equals(id)) {
            return Mono.error(new HttpClientErrorException(HttpStatus.FORBIDDEN));
        }

        return getNotificationById(id, token)
                .flatMap(notification -> {
                    if (!notification.getReceiverId().equals(UUID.fromString(decodedJWT.getSubject()))) {
                        return Mono.error(new HttpClientErrorException(HttpStatus.FORBIDDEN));
                    }
                    return notificationRepository.deleteById(UUID.fromString(id));
                })
                .switchIfEmpty(Mono.error(new HttpClientErrorException(HttpStatus.NOT_FOUND)));
    }

}
