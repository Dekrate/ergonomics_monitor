package pl.dekrate.ergonomicsmonitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("activities")
public class Activity {

    @Id
    private Long id;
    private ActivityType type;
    private LocalDateTime timestamp;
    private ActivitySummary summary;

    public Activity() {}

    public Activity(Long id, ActivityType type, LocalDateTime timestamp, ActivitySummary summary) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.summary = summary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public ActivitySummary getSummary() { return summary; }
    public void setSummary(ActivitySummary summary) { this.summary = summary; }
}
