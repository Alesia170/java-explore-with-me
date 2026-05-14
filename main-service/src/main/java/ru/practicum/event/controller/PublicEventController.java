package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatsClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.PublicEventsParams;
import ru.practicum.event.EventService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Slf4j
public class PublicEventController {

    private final EventService eventService;
    private final StatsClient statsClient;

    @GetMapping
    public List<EventShortDto> getEventsByPublic(@Valid PublicEventsParams params,
                                                 HttpServletRequest request) {
        saveHit(request);

        return eventService.getEventsByPublic(params);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventByPublic(@PathVariable @Positive Long id, HttpServletRequest request) {
        saveHit(request);

        return eventService.getEventByPublic(id);
    }

    private void saveHit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ip = request.getRemoteAddr();

        log.info("Публичный запрос  uri = {}, ip = {}", uri, ip);

        try {
            statsClient.create(uri, ip);
        } catch (RuntimeException e) {
            log.warn("Не удалось сохранить статистику для uri {}, ip {}", uri, ip, e);
        }
    }
}
