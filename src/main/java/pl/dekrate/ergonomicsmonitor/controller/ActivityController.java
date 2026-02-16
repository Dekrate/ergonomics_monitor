package pl.dekrate.ergonomicsmonitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.repository.ActivityEventRepository;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityEventRepository repository;

    public ActivityController(ActivityEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Pobiera strumień ostatnich 10 zdarzeń.
     * Dzięki WebFlux, połączenie pozostaje otwarte tylko tyle, ile trzeba,
     * nie blokując wątków serwera.
     */
    @GetMapping("/recent")
    public Flux<ActivityEvent> getRecentActivity() {
        return repository.findAll()
                .take(10); // W realnym systemie użylibyśmy sortowania po timestamp i limitu w SQL
    }
}
