package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatsClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventSort;
import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.event.EventService;

import java.time.LocalDateTime;
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
    public List<EventShortDto> getEventsByPublic(@RequestParam(required = false) String text,
                                                 @RequestParam(required = false, name = "categories")
                                                 List<Long> categoryIds,
                                                 @RequestParam(required = false) Boolean paid,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                 LocalDateTime rangeStart,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                 LocalDateTime rangeEnd,
                                                 @RequestParam(defaultValue = "false") boolean onlyAvailable,
                                                 @RequestParam(required = false) EventSort sort,
                                                 @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                 @RequestParam(defaultValue = "10") @Positive int size,
                                                 HttpServletRequest request) {
        saveHit(request);

        return eventService.getEventsByPublic(text, categoryIds, paid, rangeStart, rangeEnd, onlyAvailable,
                sort, from, size);
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
            statsClient.create(new EndpointHitRequestDto(
                    "ewm-main-service",
                    uri,
                    ip,
                    LocalDateTime.now()
            ));
        } catch (RuntimeException e) {
            log.warn("Не удалось сохранить статистику для uri {}, ip {}", uri, ip, e);
        }
    }
}
