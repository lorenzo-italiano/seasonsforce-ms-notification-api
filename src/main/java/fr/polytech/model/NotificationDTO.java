package fr.polytech.model;

import java.util.Date;
import java.util.UUID;

public class NotificationDTO {
    private String id;

    private Date date;

    private Category category;

    private String message;

    private UUID objectId;
    private UUID receiverId;

    public NotificationDTO() {
    }

    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.date = notification.getDate();
        this.category = notification.getCategory();
        this.message = notification.getMessage();
        this.objectId = UUID.fromString(notification.getObjectId());
        this.receiverId = UUID.fromString(notification.getReceiverId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    @Override
    public String toString() {
        return "NotificationDTO{" + "id=" + id + ", date=" + date + ", category=" + category + ", message='" + message + '\'' + ", objectId=" + objectId + ", receiverId=" + receiverId + '}';
    }
}
