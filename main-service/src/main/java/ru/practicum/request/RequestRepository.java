package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.dto.event.request.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long userId);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findByEventIdAndEventInitiatorIdOrderByIdAsc(Long eventId, Long userId);

    List<ParticipationRequest> findByIdInAndEventId(List<Long> requestIds, Long eventId);

    int countByEventIdAndStatus(Long eventId, RequestStatus status);
}
