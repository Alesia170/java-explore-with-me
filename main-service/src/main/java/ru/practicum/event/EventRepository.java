package ru.practicum.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByCategoryId(Long catId);

    @Query(
            value = """
                    SELECT *
                    FROM events
                    WHERE initiator_id = :userId
                    ORDER BY id
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Event> findByUserIdWithOffset(@Param("userId") Long userId,
                                       @Param("from") int from,
                                       @Param("size") int size);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    @Query(
            value = """
                    SELECT *
                    FROM events e
                    WHERE (:userIdsIsEmpty = true OR e.initiator_id IN (:userIds))
                      AND (:statesIsEmpty = true OR e.state IN (:states))
                      AND (:categoriesIsEmpty = true OR e.category_id IN (:categories))
                      AND (:rangeStart IS NULL OR e.event_date >= :rangeStart)
                      AND (:rangeEnd IS NULL OR e.event_date <= :rangeEnd)
                    ORDER BY e.id
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Event> getEventsByAdmin(@Param("userIds") List<Long> userIds,
                                 @Param("userIdsIsEmpty") boolean userIdsIsEmpty,
                                 @Param("states") List<String> states,
                                 @Param("statesIsEmpty") boolean statesIsEmpty,
                                 @Param("categories") List<Long> categories,
                                 @Param("categoriesIsEmpty") boolean categoriesIsEmpty,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("from") int from,
                                 @Param("size") int size);

    @Query(
            value = """
                    SELECT *
                    FROM events
                    WHERE state = 'PUBLISHED'
                      AND (
                            :textIsBlank = true
                            OR LOWER(annotation) LIKE LOWER(CONCAT('%', :text, '%'))
                            OR LOWER(description) LIKE LOWER(CONCAT('%', :text, '%'))
                      )
                      AND (:categoriesIsEmpty = true OR category_id IN (:categories))
                      AND (:paidIsNull OR paid = :paid)
                      AND event_date >= :rangeStart
                      AND event_date <= :rangeEnd
                      AND (
                            :onlyAvailable = false
                            OR participant_limit = 0
                            OR confirmed_requests < participant_limit
                      )
                    ORDER BY
                      CASE WHEN :sort = 'EVENT_DATE' THEN event_date END ASC,
                      CASE WHEN :sort = 'VIEWS' THEN views END DESC,
                      id ASC
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Event> getEventsByPublic(@Param("text") String text,
                                  @Param("textIsBlank") boolean textIsBlank,
                                  @Param("categories") List<Long> categories,
                                  @Param("categoriesIsEmpty") boolean categoriesIsEmpty,
                                  @Param("paid") Boolean paid,
                                  @Param("paidIsNull") boolean paidIsNull,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  @Param("onlyAvailable") boolean onlyAvailable,
                                  @Param("sort") String sort,
                                  @Param("from") int from,
                                  @Param("size") int size);

    @Query(
            value = """
                    SELECT *
                    FROM events
                    WHERE id = :id
                      AND state = 'PUBLISHED'
                    """,
            nativeQuery = true
    )
    Optional<Event> getEventByPublic(@Param("id") Long id);
}
