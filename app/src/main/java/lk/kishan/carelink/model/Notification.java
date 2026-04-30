package lk.kishan.carelink.model;

public class Notification {
    private long id;
    private String title;
    private String body;
    private String type;
    private boolean isRead;
    private String timestamp;

    public Notification() {
    }

    public Notification(long id, String title, String body, String type, boolean isRead, String timestamp) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.type = type;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}