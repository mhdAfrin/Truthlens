package lk.truthlens.controller;

import java.security.Principal;
import java.util.UUID;
import lk.truthlens.dto.NewsInput;
import lk.truthlens.service.NewsService;
import org.springframework.web.bind.annotation.*;

@RestController
public class NewsController {

    private final NewsService news;

    public NewsController(NewsService news) {
        this.news = news;
    }

    @GetMapping("/api/news")
    public Object latest() {
        return news.latest();
    }

    @GetMapping("/api/admin/news")
    public Object manage() {
        return news.manage();
    }

    @PostMapping("/api/admin/news")
    public Object create(Principal principal, @RequestBody NewsInput input) {
        return news.create(principal.getName(), input);
    }

    @PutMapping("/api/admin/news/{id}")
    public Object update(Principal principal, @PathVariable UUID id, @RequestBody NewsInput input) {
        return news.update(principal.getName(), id, input);
    }
}
