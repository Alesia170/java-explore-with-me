package ru.practicum.event;

import ru.practicum.dto.event.*;
import ru.practicum.dto.event.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.request.ParticipationRequestDto;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.dto.event.update.UpdateEventUserRequest;

import java.util.List;

public interface EventService {

    List<EventFullDto> getEventsByUser(Long userId, int from, int size);

    EventFullDto createNewEvent(Long userId, NewEventDto eventDto);

    EventFullDto getEventByUser(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    List<EventFullDto> getEventsByAdmin(AdminEventParams params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsByPublic(PublicEventsParams params);

    EventFullDto getEventByPublic(Long id);
}
