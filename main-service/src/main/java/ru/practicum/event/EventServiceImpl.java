package ru.practicum.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.request.ParticipationRequestDto;
import ru.practicum.dto.event.request.RequestStatus;
import ru.practicum.dto.event.update.AdminStateAction;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.dto.event.update.UpdateEventUserRequest;
import ru.practicum.dto.event.update.UserStateAction;
import ru.practicum.dto.stats.ViewStatsDto;
import ru.practicum.event.location.LocationMapper;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.ParticipationRequest;
import ru.practicum.request.ParticipationRequestMapper;
import ru.practicum.request.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<EventFullDto> getEventsByUser(Long userId, int from, int size) {
        getUserOrThrow(userId);

        List<Event> events = eventRepository.findByUserIdWithOffset(userId, from, size);

        return events.stream()
                .map(EventMapper::toEventFullDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto createNewEvent(Long userId, NewEventDto eventDto) {
        User initiator = getUserOrThrow(userId);
        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> {
                    log.warn("Попытка найти несущетсующую категорию");
                    return new NotFoundException("Категория с id = " + eventDto.getCategory() + " не сущствует");
                });

        if (eventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Попытка создать событие раньше чем через два часа от текущего момента");
            throw new ConflictException("Дата и время на которые намечено событие не может быть раньше, " +
                                        "чем через два часа от текущего момента");
        }

        Event event = EventMapper.toEvent(eventDto, category);
        event.setInitiator(initiator);

        Event savedEvent = eventRepository.save(event);

        log.info("Категория успешно сохранена");

        return EventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        getUserOrThrow(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        log.info("Получено событие id = {} добавленное пользователем с id = {}", eventId, userId);
        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEvent) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!(event.getState() == State.PENDING || event.getState() == State.CANCELED)) {
            log.warn("Попытка изменить, которые можно только отмененные события " +
                     "или события в состоянии ожидания модерации");
            throw new ConflictException("Изменить можно только отмененные события " +
                                        "или события в состоянии ожидания модерации");
        }

        if (updateEvent.getEventDate() != null &&
            updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата и время, на которые намечено событие, " +
                                        "не могут быть раньше чем через два часа от текущего момента");
        }

        if (updateEvent.getAnnotation() != null) {
            event.setAnnotation(updateEvent.getAnnotation());
        }

        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() ->
                            new NotFoundException("Категория с id = " + updateEvent.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateEvent.getDescription() != null) {
            event.setDescription(updateEvent.getDescription());
        }

        if (updateEvent.getTitle() != null) {
            event.setTitle(updateEvent.getTitle());
        }

        if (updateEvent.getEventDate() != null) {
            event.setEventDate(updateEvent.getEventDate());
        }

        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }

        if (updateEvent.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }

        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }

        if (updateEvent.getLocation() != null) {
            event.setLocation(LocationMapper.toLocationEntity(updateEvent.getLocation()));
        }

        if (UserStateAction.CANCEL_REVIEW.equals(updateEvent.getStateAction())) {
            event.setState(State.CANCELED);
        } else if (UserStateAction.SEND_TO_REVIEW.equals(updateEvent.getStateAction())) {
            event.setState(State.PENDING);
        }

        log.info("Обновлено событие id = {} пользователем id = {}", eventId, userId);

        return EventMapper.toEventFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        getEventOrThrow(eventId);
        getUserOrThrow(userId);

        List<ParticipationRequest> requests = requestRepository
                .findByEventIdAndEventInitiatorIdOrderByIdAsc(eventId, userId);

        log.info("Получено запросов {} на участие в событии текущего пользователя", requests.size());

        return requests.stream()
                .map(ParticipationRequestMapper::toParticipationDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        getUserOrThrow(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Событие с id = " + eventId + " не найдено или недоступно"));

        if (request.getRequestIds() == null || request.getRequestIds().isEmpty()) {
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }

        List<ParticipationRequest> requests =
                requestRepository.findByIdInAndEventId(request.getRequestIds(), eventId);

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Одна или несколько заявок не найдены");
        }

        for (ParticipationRequest participationRequest : requests) {
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии PENDING");
            }
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        if (request.getStatus() == RequestStatus.REJECTED) {
            for (ParticipationRequest participationRequest : requests) {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(participationRequest);
            }

            return toUpdateResult(confirmedRequests, rejectedRequests);
        }

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            int participantLimit = event.getParticipantLimit();

            int confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (participantLimit == 0 || !event.getRequestModeration()) {
                for (ParticipationRequest participationRequest : requests) {
                    participationRequest.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(participationRequest);
                    confirmedCount++;
                }

                event.setConfirmedRequests(confirmedCount);

                return toUpdateResult(confirmedRequests, rejectedRequests);
            }

            if (confirmedCount >= participantLimit) {
                throw new ConflictException("Лимит участников события уже достигнут");
            }

            for (ParticipationRequest participationRequest : requests) {
                if (confirmedCount < participantLimit) {
                    participationRequest.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(participationRequest);
                    confirmedCount++;
                } else {
                    participationRequest.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(participationRequest);
                }
            }
            event.setConfirmedRequests(confirmedCount);
            return toUpdateResult(confirmedRequests, rejectedRequests);
        }

        throw new ConflictException("Некорректный статус заявки");
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> userIds, List<State> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        boolean userIdsIsEmpty = userIds == null || userIds.isEmpty();
        boolean statesIsEmpty = states == null || states.isEmpty();
        boolean categoriesIsEmpty = categories == null || categories.isEmpty();

        List<Long> safeUserIds = userIdsIsEmpty ? List.of(-1L) : userIds;
        List<Long> safeCategories = categoriesIsEmpty ? List.of(-1L) : categories;
        List<String> safeStates = statesIsEmpty ? List.of("__EMPTY__") :
                states.stream()
                        .map(Enum::name)
                        .toList();

        List<Event> events = eventRepository.getEventsByAdmin(
                safeUserIds,
                userIdsIsEmpty,
                safeStates,
                statesIsEmpty,
                safeCategories,
                categoriesIsEmpty,
                rangeStart,
                rangeEnd,
                from,
                size
        );

        Map<String, Long> views = getViewsFromStats(
                events.stream()
                        .map(event -> "/events/" + event.getId())
                        .toList()
        );

        for (Event event : events) {
            String uri = "/events/" + event.getId();
            event.setViews(views.getOrDefault(uri, 0L));
        }

        log.info("Получено {} о событиих подходящих под условия", events.size());

        return events.stream()
                .map(EventMapper::toEventFullDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventOrThrow(eventId);

        if (request.getEventDate() != null &&
            request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата и время, на которые намечено событие, " +
                                        "не могут быть раньше чем через два часа от текущего момента");
        }

        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (!event.getState().equals(State.PENDING)) {
                throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
            }

            event.setState(State.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }

        if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == State.PUBLISHED) {
                throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
            }

            event.setState(State.CANCELED);
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() ->
                            new NotFoundException("Категория с id = " + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getLocation() != null) {
            event.setLocation(LocationMapper.toLocationEntity(request.getLocation()));
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        return EventMapper.toEventFullDto(event);
    }

    @Override
    public List<EventShortDto> getEventsByPublic(String text, List<Long> categoryIds, Boolean paid, LocalDateTime rangeStart,
                                                 LocalDateTime rangeEnd, boolean onlyAvailable,
                                                 EventSort sort, int from, int size) {
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart не может быть позже rangeEnd");
        }

        boolean textIsBlank = text == null || text.isBlank();
        String safeText = textIsBlank ? "" : text;

        boolean paidIsNull = paid == null;
        Boolean safePaid = !paidIsNull && paid;

        LocalDateTime safeRangeEnd = rangeEnd == null
                ? LocalDateTime.of(2099, 12, 31, 23, 59, 59)
                : rangeEnd;

        boolean categoriesIsEmpty = categoryIds == null || categoryIds.isEmpty();
        List<Long> safeCategories = categoriesIsEmpty ? List.of(-1L) : categoryIds;
        String sortValue = sort == null ? "EVENT_DATE" : sort.name();

        List<Event> events = eventRepository.getEventsByPublic(
                safeText,
                textIsBlank,
                safeCategories,
                categoriesIsEmpty,
                safePaid,
                paidIsNull,
                rangeStart,
                safeRangeEnd,
                onlyAvailable,
                sortValue,
                from,
                size
        );

        Map<String, Long> views = getViewsFromStats(
                events.stream()
                        .map(event -> "/events/" + event.getId())
                        .toList()
        );

        for (Event event : events) {
            String uri = "/events/" + event.getId();
            event.setViews(views.getOrDefault(uri, 0L));
        }

        log.info("Получено {} событий по заданным фильтрам", events.size());

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    @Override
    public EventFullDto getEventByPublic(Long eventId) {
        Event event = eventRepository.getEventByPublic(eventId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти не существующее событие");
                    return new NotFoundException("Событие с id = " + eventId + " не существует");
                });

        int confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        Long views = getViewsFromStats("/events/" + eventId);

        event.setConfirmedRequests(confirmedRequests);
        event.setViews(views);

        log.info("Получено публичное событие id = {}, views = {}, confirmedRequests = {}",
                eventId, views, confirmedRequests);
        return EventMapper.toEventFullDto(event);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующего пользователя");
                    return new NotFoundException("Пользователь с id = " + userId + " не найден");
                });
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующее событие");
                    return new NotFoundException("Событие с id = " + eventId + " не существует");
                });
    }

    private EventRequestStatusUpdateResult toUpdateResult(List<ParticipationRequest> confirmedRequests,
                                                          List<ParticipationRequest> rejectedRequests) {
        return new EventRequestStatusUpdateResult(
                confirmedRequests.stream()
                        .map(ParticipationRequestMapper::toParticipationDto)
                        .toList(),
                rejectedRequests.stream()
                        .map(ParticipationRequestMapper::toParticipationDto)
                        .toList()
        );
    }

    private Long getViewsFromStats(String uri) {
        return getViewsFromStats(List.of(uri)).getOrDefault(uri, 0L);
    }

    private Map<String, Long> getViewsFromStats(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return Map.of();
        }

        ResponseEntity<Object> response = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                true
        );

        if (response.getBody() == null) {
            return Map.of();
        }

        List<ViewStatsDto> stats = objectMapper.convertValue(
                response.getBody(),
                new TypeReference<>() {
                }
        );

        return stats.stream()
                .collect(Collectors.toMap(
                        ViewStatsDto::getUri,
                        ViewStatsDto::getHits
                ));
    }
}
