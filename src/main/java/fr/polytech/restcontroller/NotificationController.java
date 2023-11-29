package fr.polytech.restcontroller;

import fr.polytech.annotation.IsAdmin;
import fr.polytech.annotation.IsSender;
import fr.polytech.model.Notification;
import fr.polytech.service.NotificationService;
import jakarta.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {

    /**
     * Initialize logger
     */
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get all notifications.
     *
     * @return A flux of notifications.
     */
    @GetMapping("/")
    @IsAdmin
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Flux<Notification> getNotification() {
        return notificationService.getAllNotifications()
                .doOnComplete(() -> logger.info("Successfully got all notifications"))
                .doOnError(e -> logger.error("Error while getting all notifications", e));
    }

    /**
     * Get a notification by its id.
     *
     * @param id The id of the notification.
     * @return The notification.
     */
    @GetMapping("/{id}")
    @IsAdmin
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Mono<Notification> getNotificationById(@PathVariable String id) {
        return notificationService.getNotificationById(id)
                .doOnSuccess(notification -> logger.info("Got notification with id: {}", id))
                .doOnError(e -> logger.error("Error while getting the notification with id: {}", id, e))
                .switchIfEmpty(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, null)));
    }

    /**
     * Get all notifications by receiver id.
     *
     * @param userId The id of the receiver.
     * @param token  The token of the sender.
     * @return A flux of notifications.
     */
    @GetMapping("/user/{userId}")
    @IsSender
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Flux<Notification> getNotificationByReceiverId(@PathVariable String userId, @RequestHeader("Authorization") String token) {
        return notificationService.getAllNotificationsByReceiverId(userId)
                .doOnComplete(() -> logger.info("Successfully got all notifications by receiver id"))
                .doOnError(e -> logger.error("Error while getting all notifications by receiver id", e));
    }

    /**
     * Delete a notification by its id.
     *
     * @param id    The id of the notification.
     * @param token The token of the sender.
     * @return A boolean indicating if the notification has been deleted.
     */
    @DeleteMapping("/{id}")
    @Produces(MediaType.TEXT_PLAIN_VALUE)
    public Mono<Boolean> removeNotification(@PathVariable String id, @RequestHeader("Authorization") String token) {
        return notificationService.deleteNotification(id, token)
                .thenReturn(true)
                .doOnSuccess(aVoid -> logger.info("Notification removed successfully"))
                .doOnError(e -> logger.error("Error while removing notification", e))
                .switchIfEmpty(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, null)));
    }

}
