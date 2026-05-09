package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.dto.stats.EndpointHitResponseDto;
import ru.practicum.dto.stats.ViewStatsDto;
import ru.practicum.server.EndpointHit;
import ru.practicum.server.EndpointHitMapper;
import ru.practicum.server.StatsRepository;
import ru.practicum.server.StatsServiceImpl;
import ru.practicum.server.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
    }

    @Test
    void create_shouldSaveEndpointAndReturnResponseDto() {
        EndpointHitRequestDto requestDto =
                new EndpointHitRequestDto(
                        "ewm-main-service",
                        "/events",
                        "192.168.0.1",
                        LocalDateTime.of(2024, 1, 1, 12, 0, 0)
                );

        EndpointHit endpointHit = new EndpointHit();
        endpointHit.setApp("ewm-main-service");
        endpointHit.setUri("/events");
        endpointHit.setIp("192.168.0.1");
        endpointHit.setTimestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0));

        EndpointHit saved = new EndpointHit();
        saved.setId(1L);
        saved.setApp("ewm-main-service");
        saved.setUri("/events");
        saved.setIp("192.168.0.1");
        saved.setTimestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0));

        EndpointHitResponseDto responseDto = new EndpointHitResponseDto(
                1L,
                "ewm-main-service",
                "/events",
                "192.168.0.1",
                LocalDateTime.of(2024, 1, 1, 12, 0, 0)
        );

        try (MockedStatic<EndpointHitMapper> mapperMock = mockStatic(EndpointHitMapper.class)) {
            mapperMock.when(() -> EndpointHitMapper.toEndpoint(requestDto)).thenReturn(endpointHit);
            when(statsRepository.save(endpointHit)).thenReturn(saved);
            mapperMock.when(() -> EndpointHitMapper.toEndpointDto(saved)).thenReturn(responseDto);

            EndpointHitResponseDto result = statsService.create(requestDto);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("ewm-main-service", result.getApp());
            assertEquals("/events", result.getUri());
            assertEquals("192.168.0.1", result.getIp());

            verify(statsRepository).save(endpointHit);
        }
    }

    @Test
    void getStatsWhenUniqueFalseAndUrisNull() {
        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 10L)
        );

        when(statsRepository.getStats(start, end)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, null, false);

        assertEquals(expected, result);
        verify(statsRepository).getStats(start, end);
        verify(statsRepository, never()).getUniqueStats(any(), any());
        verify(statsRepository, never()).getStatsByUris(any(), any(), any());
        verify(statsRepository, never()).getUniqueStatsByUris(any(), any(), any());
    }

    @Test
    void getStatsWhenUniqueTrueAndUrisNull() {
        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 5L)
        );

        when(statsRepository.getUniqueStats(start, end)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, null, true);

        assertEquals(expected, result);
        verify(statsRepository).getUniqueStats(start, end);
        verify(statsRepository, never()).getStats(any(), any());
        verify(statsRepository, never()).getStatsByUris(any(), any(), any());
        verify(statsRepository, never()).getUniqueStatsByUris(any(), any(), any());
    }

    @Test
    void getStatsWhenUniqueFalseAndUrisPresent() {
        List<String> uris = List.of("/events", "/events/1");
        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 10L),
                new ViewStatsDto("ewm-main-service", "/events/1", 7L)
        );

        when(statsRepository.getStatsByUris(start, end, uris)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, uris, false);

        assertEquals(expected, result);
        verify(statsRepository).getStatsByUris(start, end, uris);
        verify(statsRepository, never()).getStats(any(), any());
        verify(statsRepository, never()).getUniqueStats(any(), any());
        verify(statsRepository, never()).getUniqueStatsByUris(any(), any(), any());
    }

    @Test
    void getStatsWhenUniqueTrueAndUrisPresent() {
        List<String> uris = List.of("/events", "/events/1");
        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 5L),
                new ViewStatsDto("ewm-main-service", "/events/1", 3L)
        );

        when(statsRepository.getUniqueStatsByUris(start, end, uris)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, uris, true);

        assertEquals(expected, result);
        verify(statsRepository).getUniqueStatsByUris(start, end, uris);
        verify(statsRepository, never()).getStats(any(), any());
        verify(statsRepository, never()).getUniqueStats(any(), any());
        verify(statsRepository, never()).getStatsByUris(any(), any(), any());
    }

    @Test
    void getStatsWhenUrisEmpty() {
        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 10L)
        );

        when(statsRepository.getStats(start, end)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, List.of(), false);

        assertEquals(expected, result);
        verify(statsRepository).getStats(start, end);
        verify(statsRepository, never()).getStatsByUris(any(), any(), any());
    }

    @Test
    void getStatsWhenStartAfterEnd() {
        LocalDateTime invalidStart = LocalDateTime.of(2024, 1, 3, 0, 0, 0);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> statsService.getStats(invalidStart, end, null, false)
        );

        assertEquals("start не может быть после end", ex.getMessage());
        verifyNoInteractions(statsRepository);
    }
}

