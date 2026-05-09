package ru.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.State;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEventsByAdmin(@RequestParam(required = false, name = "users") List<Long> userIds,
                                               @RequestParam(required = false) List<State> states,
                                               @RequestParam(required = false) List<Long> categories,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                               LocalDateTime rangeStart,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                               LocalDateTime rangeEnd,
                                               @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                               @RequestParam(defaultValue = "10") @Positive int size) {
        return eventService.getEventsByAdmin(userIds, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable @Positive Long eventId,
                                           @RequestBody UpdateEventAdminRequest request) {
        return eventService.updateEventByAdmin(eventId, request);
    }
}
