package ru.practicum.request;

import ru.practicum.dto.event.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
