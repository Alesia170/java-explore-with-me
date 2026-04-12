package ru.practicum.server;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitRequestDto;
import ru.practicum.dto.EndpointHitResponseDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class StatsController {
    private final StatsService statsService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitResponseDto create(@Valid @RequestBody EndpointHitRequestDto dto) {
        return statsService.create(dto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam String start,
                                       @RequestParam String end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") boolean unique) {

        LocalDateTime startTime = parseDateTime(start, "start");
        LocalDateTime endTime = parseDateTime(end, "end");

        return statsService.getStats(startTime, endTime, uris, unique);
    }

    private LocalDateTime parseDateTime(String value, String paramName) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неправильный формат даты '" + paramName + "'. Ожидалось: yyyy-MM-dd HH:mm:ss"
            );
        }
    }
}
