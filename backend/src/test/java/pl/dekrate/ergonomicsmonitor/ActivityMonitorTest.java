package pl.dekrate.ergonomicsmonitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;

/**
 * Unit test for ActivityMonitor logic. Focuses on event aggregation without starting native hooks
 * or database.
 */
class ActivityMonitorTest {

    private final ActivityRepository repository = mock(ActivityRepository.class);
    private final ActivityMonitor monitor = new ActivityMonitor(repository);

    @Test
    void shouldCorrectlyAggregateEvents() {
        // Given
        List<ActivityType> types =
                List.of(
                        ActivityType.KEYBOARD,
                        ActivityType.KEYBOARD,
                        ActivityType.MOUSE,
                        ActivityType.KEYBOARD);

        // When
        ActivityEvent result = monitor.createAggregatedEvent(types);

        // Then
        assertNotNull(result.getId());
        assertEquals(4.0, result.getIntensity());
        assertEquals(3L, result.getMetadata().get("keyboard_count"));
        assertEquals(1L, result.getMetadata().get("mouse_count"));
        assertEquals(4L, result.getMetadata().get("total_count"));
        assertEquals(ActivityType.SYSTEM_EVENT, result.getType());
    }
}
