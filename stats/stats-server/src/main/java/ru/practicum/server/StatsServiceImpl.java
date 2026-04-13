package ru.practicum.server;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHitRequestDto;
import ru.practicum.dto.EndpointHitResponseDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    public EndpointHitResponseDto create(EndpointHitRequestDto dto) {
        EndpointHit hit = EndpointHitMapper.toEndpoint(dto);

        return EndpointHitMapper.toEndpointDto(statsRepository.save(hit));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {

        if (start.isAfter(end)) {
            throw new ValidationException("start не может быть после end");
        }

        boolean hasUris = uris != null && !uris.isEmpty();

        if (unique) {
            return hasUris
                    ? statsRepository.getUniqueStatsByUris(start, end, uris)
                    : statsRepository.getUniqueStats(start, end);
        }

        return hasUris
                ? statsRepository.getStatsByUris(start, end, uris)
                : statsRepository.getStats(start, end);
    }
}