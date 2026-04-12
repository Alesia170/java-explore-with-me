package ru.practicum.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.EndpointHitRequestDto;
import ru.practicum.dto.EndpointHitResponseDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EndpointHitMapper {
    public static EndpointHitResponseDto toEndpointDto(EndpointHit endpointHit) {
        return new EndpointHitResponseDto(
                endpointHit.getId(),
                endpointHit.getApp(),
                endpointHit.getUri(),
                endpointHit.getIp(),
                endpointHit.getTimestamp()
        );
    }

    public static EndpointHit toEndpoint(EndpointHitRequestDto dto) {
        EndpointHit endpointHit = new EndpointHit();
        endpointHit.setApp(dto.getApp());
        endpointHit.setIp(dto.getIp());
        endpointHit.setUri(dto.getUri());
        endpointHit.setTimestamp(dto.getTimestamp());
        return endpointHit;
    }
}
