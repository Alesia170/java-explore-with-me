package ru.practicum.server;

import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.dto.stats.EndpointHitResponseDto;
import ru.practicum.dto.stats.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    EndpointHitResponseDto create(EndpointHitRequestDto dto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
