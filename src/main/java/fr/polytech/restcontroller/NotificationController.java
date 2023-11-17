package fr.polytech.restcontroller;

import fr.polytech.annotation.IsAdmin;
import fr.polytech.annotation.IsSender;
import fr.polytech.model.Notification;
import fr.polytech.service.NotificationService;
import jakarta.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/")
    @IsAdmin
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Flux<Notification> getNotification() {
        return notificationService.getAllNotifications()
                .doOnComplete(() -> logger.info("Successfully got all notifications"))
                .doOnError(e -> logger.error("Error while getting all notifications", e));
    }

    @GetMapping("/{id}")
    @IsAdmin
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Mono<Notification> getNotificationById(@PathVariable String id, @RequestHeader("Authorization") String token) {
        return notificationService.getNotificationById(id, token)
                .doOnSuccess(notification -> logger.info("Got notification with id: {}", id))
                .doOnError(e -> logger.error("Error while getting the notification with id: {}", id, e));
    }

    @GetMapping("/user/{userId}")
    @IsSender
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public Flux<Notification> getNotificationByReceiverId(@PathVariable String userId, @RequestHeader("Authorization") String token) {
        return notificationService.getAllNotificationsByReceiverId(userId)
                .doOnComplete(() -> logger.info("Successfully got all notifications by receiver id"))
                .doOnError(e -> logger.error("Error while getting all notifications by receiver id", e));
    }

    @DeleteMapping("/{id}")
    @Produces(MediaType.TEXT_PLAIN_VALUE)
    public Mono<Boolean> removeNotification(@PathVariable String id, @RequestHeader("Authorization") String token) {
        return notificationService.deleteNotification(id, token)
                .doOnSuccess(aVoid -> logger.info("Notification removed successfully"))
                .thenReturn(true)
                .onErrorResume(e -> {
                    logger.error("Error while removing the notification", e);
                    if (e instanceof Error) {
                        return Mono.error(e);
                    }
                    return Mono.just(false);
                });
    }

}
