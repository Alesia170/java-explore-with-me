package ru.practicum.server;

import ru.practicum.dto.EndpointHitRequestDto;
import ru.practicum.dto.EndpointHitResponseDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    EndpointHitResponseDto create(EndpointHitRequestDto dto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
