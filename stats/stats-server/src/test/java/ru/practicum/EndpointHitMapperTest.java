package ru.practicum;

import org.junit.jupiter.api.Test;
import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.dto.stats.EndpointHitResponseDto;
import ru.practicum.server.EndpointHit;
import ru.practicum.server.EndpointHitMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EndpointHitMapperTest {

    @Test
    void shouldMapEndpointHitToResponseDto() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 10, 12, 30, 0);

        EndpointHit endpointHit = new EndpointHit();
        endpointHit.setId(1L);
        endpointHit.setApp("ewm-main-service");
        endpointHit.setUri("/events/1");
        endpointHit.setIp("192.168.0.1");
        endpointHit.setTimestamp(timestamp);

        EndpointHitResponseDto dto = EndpointHitMapper.toEndpointDto(endpointHit);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("ewm-main-service", dto.getApp());
        assertEquals("/events/1", dto.getUri());
        assertEquals("192.168.0.1", dto.getIp());
        assertEquals(timestamp, dto.getTimestamp());
    }

    @Test
    void shouldMapRequestDtoToEndpointHit() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 10, 12, 30, 0);

        EndpointHitRequestDto dto = new EndpointHitRequestDto();
        dto.setApp("ewm-main-service");
        dto.setUri("/events/1");
        dto.setIp("192.168.0.1");
        dto.setTimestamp(timestamp);

        EndpointHit endpointHit = EndpointHitMapper.toEndpoint(dto);

        assertNotNull(endpointHit);
        assertNull(endpointHit.getId());
        assertEquals("ewm-main-service", endpointHit.getApp());
        assertEquals("/events/1", endpointHit.getUri());
        assertEquals("192.168.0.1", endpointHit.getIp());
        assertEquals(timestamp, endpointHit.getTimestamp());
    }
}
