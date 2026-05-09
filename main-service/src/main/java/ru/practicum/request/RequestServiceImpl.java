package ru.practicum.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.State;
import ru.practicum.dto.event.request.ParticipationRequestDto;
import ru.practicum.dto.event.request.RequestStatus;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.info("Получено заявок {} от текущего пользователя", requests.size());

        return requests.stream()
                .map(ParticipationRequestMapper::toParticipationDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = getUserOrThrow(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующее событие id = {}", eventId);
                    return new NotFoundException("Событие с id = " + eventId + " не найдено или недоступно");
                });
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("Попытка добавить повторный запрос на участие");
            throw new ConflictException("Нельзя добавить повторный запрос на участие");
        }

        if (event.getInitiator().getId().equals(userId)) {
            log.warn("Инициатор пытается добавить запрос на участие в своем событии");
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своем событии");
        }

        if (!State.PUBLISHED.equals(event.getState())) {
            log.warn("Попытка участвовать в неопубликованном событии");
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() != 0
            && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            log.warn("Лимит запросов достигнут предела");
            throw new ConflictException("У события достигнут лимит запросов на участие");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setRequester(requester);
        request.setEvent(event);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);

        log.info("Запрос на участие в событии создан");

        return ParticipationRequestMapper.toParticipationDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUserOrThrow(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несущестующий запрос");
                    return new NotFoundException("Запрос с id = " + requestId + " не найден");
                });

        if (!request.getStatus().equals(RequestStatus.CANCELED)) {
            request.setStatus(RequestStatus.CANCELED);
        }

        if (!request.getRequester().getId().equals(userId)) {
            log.warn("Пользователь пытается отменить чужой запрос");
            throw new ConflictException("Нельзя отменить чужой запрос на участие");
        }

        ParticipationRequest updatedRequest = requestRepository.save(request);

         log.info("Отмена своего запроса на участие в событии");

        return ParticipationRequestMapper.toParticipationDto(updatedRequest);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующего пользователя");
                    return new NotFoundException("Пользователь с id = " + userId + " не найден");
                });
    }
}
