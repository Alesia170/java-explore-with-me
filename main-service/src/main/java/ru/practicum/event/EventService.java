package ru.practicum.event;

import ru.practicum.dto.event.*;
import ru.practicum.dto.event.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.request.ParticipationRequestDto;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.dto.event.update.UpdateEventUserRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    List<EventFullDto> getEventsByUser(Long userId, int from, int size);

    EventFullDto createNewEvent(Long userId, NewEventDto eventDto);

    EventFullDto getEventByUser(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    List<EventFullDto> getEventsByAdmin(List<Long> userIds, List<State> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsByPublic(String text, List<Long> categoryIds, Boolean paid, LocalDateTime rangeStart,
                                          LocalDateTime rangeEnd, boolean onlyAvailable,
                                          EventSort sort, int from, int size);

    EventFullDto getEventByPublic(Long id);
}
