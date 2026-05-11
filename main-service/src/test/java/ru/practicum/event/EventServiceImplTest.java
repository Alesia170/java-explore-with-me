package ru.practicum.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.practicum.StatsClient;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.update.AdminStateAction;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.dto.event.update.UpdateEventUserRequest;
import ru.practicum.dto.event.update.UserStateAction;
import ru.practicum.dto.stats.ViewStatsDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatsClient statsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("User");
        user.setEmail("user@mail.com");

        category = new Category();
        category.setId(1L);
        category.setName("Concerts");

        event = new Event();
        event.setId(1L);
        event.setTitle("Test event");
        event.setAnnotation("Test annotation");
        event.setDescription("Test description");
        event.setCategory(category);
        event.setInitiator(user);
        event.setPaid(false);
        event.setEventDate(LocalDateTime.now().plusDays(3));
        event.setCreatedOn(LocalDateTime.now());
        event.setPublishedOn(LocalDateTime.now());
        event.setParticipantLimit(10);
        event.setRequestModeration(true);
        event.setConfirmedRequests(0);
        event.setViews(0L);
        event.setState(State.PENDING);

        ru.practicum.event.location.Location location = new ru.practicum.event.location.Location();
        location.setLat(55.75F);
        location.setLon(37.61F);
        event.setLocation(location);
    }

    @Test
    void shouldGetEventsByUser() {
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByUserIdWithOffset(user.getId(), 0, 10))
                .thenReturn(List.of(event));

        List<EventFullDto> result = eventService.getEventsByUser(user.getId(), 0, 10);

        assertEquals(1, result.size());
        assertEquals(event.getId(), result.getFirst().getId());
        assertEquals(event.getTitle(), result.getFirst().getTitle());

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByUserIdWithOffset(user.getId(), 0, 10);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.getEventsByUser(99L, 0, 10));

        verify(userRepository).findById(99L);
        verify(eventRepository, never()).findByUserIdWithOffset(anyLong(), anyInt(), anyInt());
    }

    @Test
    void shouldCreateNewEvent() {
        NewEventDto request = new NewEventDto();
        request.setTitle("New event");
        request.setAnnotation("Annotation");
        request.setDescription("Description");
        request.setCategory(category.getId());
        request.setPaid(false);
        request.setEventDate(LocalDateTime.now().plusDays(3));
        request.setParticipantLimit(10);
        request.setRequestModeration(true);

        Location location = new Location();
        location.setLat(55.75F);
        location.setLon(37.61F);
        request.setLocation(location);

        ru.practicum.event.location.Location location1 = new ru.practicum.event.location.Location();
        location.setLat(55.75F);
        location.setLon(37.61F);

        Event savedEvent = new Event();
        savedEvent.setId(2L);
        savedEvent.setTitle(request.getTitle());
        savedEvent.setAnnotation(request.getAnnotation());
        savedEvent.setDescription(request.getDescription());
        savedEvent.setCategory(category);
        savedEvent.setInitiator(user);
        savedEvent.setPaid(request.getPaid());
        savedEvent.setEventDate(request.getEventDate());
        savedEvent.setParticipantLimit(request.getParticipantLimit());
        savedEvent.setRequestModeration(request.getRequestModeration());
        savedEvent.setLocation(location1);
        savedEvent.setState(State.PENDING);
        savedEvent.setConfirmedRequests(0);
        savedEvent.setViews(0L);
        savedEvent.setCreatedOn(LocalDateTime.now());

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(savedEvent);

        EventFullDto result = eventService.createNewEvent(user.getId(), request);

        assertEquals(savedEvent.getId(), result.getId());
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(category.getId(), result.getCategory().getId());
        assertEquals(user.getId(), result.getInitiator().getId());

        verify(userRepository).findById(user.getId());
        verify(categoryRepository).findById(category.getId());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void shouldNotCreateNewEventWhenUserNotFoundThenThrowNotFoundException() {
        NewEventDto request = new NewEventDto();
        request.setCategory(category.getId());
        request.setEventDate(LocalDateTime.now().plusDays(3));

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.createNewEvent(99L, request));

        verify(userRepository).findById(99L);
        verify(categoryRepository, never()).findById(anyLong());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewEventWhenCategoryNotFoundThenThrowNotFoundException() {
        NewEventDto request = new NewEventDto();
        request.setCategory(99L);
        request.setEventDate(LocalDateTime.now().plusDays(3));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.createNewEvent(user.getId(), request));

        verify(userRepository).findById(user.getId());
        verify(categoryRepository).findById(99L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewEventWhenEventDateTooEarlyThenThrowConflictException() {
        NewEventDto request = new NewEventDto();
        request.setCategory(category.getId());
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        assertThrows(ValidationException.class,
                () -> eventService.createNewEvent(user.getId(), request));

        verify(userRepository).findById(user.getId());
        verify(categoryRepository).findById(category.getId());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldGetEventByUser() {
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event.getId(), user.getId()))
                .thenReturn(Optional.of(event));

        EventFullDto result = eventService.getEventByUser(user.getId(), event.getId());

        assertEquals(event.getId(), result.getId());
        assertEquals(event.getTitle(), result.getTitle());

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(event.getId(), user.getId());
    }

    @Test
    void getEventByUserWhenEventNotFoundThenThrowNotFoundException() {
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(99L, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.getEventByUser(user.getId(), 99L));

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(99L, user.getId());
    }

    @Test
    void shouldUpdateEvent() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title title");
        request.setAnnotation("Updated annotation annotation");
        request.setStateAction(UserStateAction.CANCEL_REVIEW);

        event.setState(State.PENDING);

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event.getId(), user.getId()))
                .thenReturn(Optional.of(event));

        EventFullDto result = eventService.updateEvent(user.getId(), event.getId(), request);

        assertEquals("Updated title title", result.getTitle());
        assertEquals("Updated annotation annotation", result.getAnnotation());
        assertEquals(State.CANCELED, result.getState());

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(event.getId(), user.getId());
    }

    @Test
    void shouldUpdateEventWhenEventIsPublishedThenThrowConflictException() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title Updated title");

        event.setState(State.PUBLISHED);

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event.getId(), user.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateEvent(user.getId(), event.getId(), request));

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(event.getId(), user.getId());
    }

    @Test
    void shouldUpdateEventWhenEventDateTooEarlyThenThrowConflictException() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        event.setState(State.PENDING);

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event.getId(), user.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(ValidationException.class,
                () -> eventService.updateEvent(user.getId(), event.getId(), request));

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(event.getId(), user.getId());
    }

    @Test
    void shouldUpdateEventWhenNewCategoryNotFoundThenThrowNotFoundException() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setCategory(99L);

        event.setState(State.PENDING);

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event.getId(), user.getId()))
                .thenReturn(Optional.of(event));
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.updateEvent(user.getId(), event.getId(), request));

        verify(userRepository).findById(user.getId());
        verify(eventRepository).findByIdAndInitiatorId(event.getId(), user.getId());
        verify(categoryRepository).findById(99L);
    }

    @Test
    void shouldGetEventsByAdminWhenEventsFoundThenReturnEventsWithViews() {
        event.setState(State.PUBLISHED);

        ViewStatsDto stat = new ViewStatsDto();
        stat.setApp("ewm-main-service");
        stat.setUri("/events/" + event.getId());
        stat.setHits(5L);

        when(eventRepository.getEventsByAdminWithoutEndDate(
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                any(LocalDateTime.class),
                eq(0),
                eq(10)
        )).thenReturn(List.of(event));

        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(ResponseEntity.ok(List.of(stat)));

        doReturn(List.of(stat))
                .when(objectMapper)
                .convertValue(any(), ArgumentMatchers.<TypeReference<List<ViewStatsDto>>>any());

        List<EventFullDto> result = eventService.getEventsByAdmin(
                null,
                null,
                null,
                null,
                null,
                0,
                10
        );

        assertEquals(1, result.size());
        assertEquals(5L, result.getFirst().getViews());

        verify(eventRepository).getEventsByAdminWithoutEndDate(
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                any(LocalDateTime.class),
                eq(0),
                eq(10)
        );
        verify(statsClient).getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true));
    }

    @Test
    void shouldGetEventsByAdminWhenNoEventsFoundThenReturnEmptyList() {
        when(eventRepository.getEventsByAdminWithoutEndDate(
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                any(LocalDateTime.class),
                eq(0),
                eq(10)
        )).thenReturn(List.of());

        List<EventFullDto> result = eventService.getEventsByAdmin(
                null,
                null,
                null,
                null,
                null,
                0,
                10
        );

        assertTrue(result.isEmpty());

        verify(statsClient, never()).getStats(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void shouldGetEventsByAdminWithRangeEndWhenEventsFoundThenReturnEventsWithViews() {
        event.setState(State.PUBLISHED);

        LocalDateTime rangeStart = LocalDateTime.now().minusDays(1);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(30);

        ViewStatsDto stat = new ViewStatsDto();
        stat.setApp("ewm-main-service");
        stat.setUri("/events/" + event.getId());
        stat.setHits(5L);

        when(eventRepository.getEventsByAdminWithEndDate(
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(0),
                eq(10)
        )).thenReturn(List.of(event));

        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(ResponseEntity.ok(List.of(stat)));

        doReturn(List.of(stat))
                .when(objectMapper)
                .convertValue(any(), ArgumentMatchers.<TypeReference<List<ViewStatsDto>>>any());

        List<EventFullDto> result = eventService.getEventsByAdmin(
                null,
                null,
                null,
                rangeStart,
                rangeEnd,
                0,
                10
        );

        assertEquals(1, result.size());
        assertEquals(5L, result.getFirst().getViews());

        verify(eventRepository).getEventsByAdminWithEndDate(
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                isNull(),
                eq(true),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(0),
                eq(10)
        );
    }

    @Test
    void shouldUpdateEventByAdmin() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);

        event.setState(State.PENDING);

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        EventFullDto result = eventService.updateEventByAdmin(event.getId(), request);

        assertEquals(State.PUBLISHED, result.getState());
        assertNotNull(result.getPublishedOn());

        verify(eventRepository).findById(event.getId());
    }

    @Test
    void shouldUpdateEventByAdminWhenPublishNotPendingEventThenThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);

        event.setState(State.CANCELED);

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(event.getId(), request));

        verify(eventRepository).findById(event.getId());
    }

    @Test
    void shouldUpdateEventByAdminWhenRejectPublishedEventThenThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.REJECT_EVENT);

        event.setState(State.PUBLISHED);

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(event.getId(), request));

        verify(eventRepository).findById(event.getId());
    }

    @Test
    void shouldUpdateEventByAdminWhenEventDateTooEarlyThenThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(ValidationException.class,
                () -> eventService.updateEventByAdmin(event.getId(), request));

        verify(eventRepository).findById(event.getId());
    }

    @Test
    void shouldGetEventsByPublic() {
        event.setState(State.PUBLISHED);

        ViewStatsDto stat = new ViewStatsDto();
        stat.setApp("ewm-main-service");
        stat.setUri("/events/" + event.getId());
        stat.setHits(7L);

        when(eventRepository.getEventsByPublic(
                any(),
                anyBoolean(),
                anyList(),
                anyBoolean(),
                any(),
                anyBoolean(),
                any(),
                any(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of(event));

        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(ResponseEntity.ok(List.of(stat)));

        doReturn(List.of(stat))
                .when(objectMapper)
                .convertValue(any(), ArgumentMatchers.<TypeReference<List<ViewStatsDto>>>any());

        List<EventShortDto> result = eventService.getEventsByPublic(
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                0,
                10
        );

        assertEquals(1, result.size());
        assertEquals(7L, result.getFirst().getViews());

        verify(eventRepository).getEventsByPublic(
                any(),
                anyBoolean(),
                anyList(),
                anyBoolean(),
                any(),
                anyBoolean(),
                any(),
                any(),
                eq(false),
                any(),
                eq(0),
                eq(10)
        );
        verify(statsClient).getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true));
    }

    @Test
    void shouldGetEventsByPublicWhenNothingFoundThenReturnEmptyList() {
        when(eventRepository.getEventsByPublic(
                any(),
                anyBoolean(),
                anyList(),
                anyBoolean(),
                any(),
                anyBoolean(),
                any(),
                any(),
                anyBoolean(),
                any(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());

        List<EventShortDto> result = eventService.getEventsByPublic(
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                0,
                10
        );

        assertTrue(result.isEmpty());

        verify(statsClient, never()).getStats(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void shouldGetEventByPublicWhenEventExists() {
        event.setState(State.PUBLISHED);

        ViewStatsDto stat = new ViewStatsDto();
        stat.setApp("ewm-main-service");
        stat.setUri("/events/" + event.getId());
        stat.setHits(10L);

        when(eventRepository.getEventByPublic(event.getId()))
                .thenReturn(Optional.of(event));
        when(requestRepository.countByEventIdAndStatus(eq(event.getId()), any()))
                .thenReturn(3);
        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true)))
                .thenReturn(ResponseEntity.ok(List.of(stat)));
        doReturn(List.of(stat))
                .when(objectMapper)
                .convertValue(any(), ArgumentMatchers.<TypeReference<List<ViewStatsDto>>>any());

        EventFullDto result = eventService.getEventByPublic(event.getId());

        assertEquals(event.getId(), result.getId());
        assertEquals(10L, result.getViews());
        assertEquals(3, result.getConfirmedRequests());

        verify(eventRepository).getEventByPublic(event.getId());
        verify(requestRepository).countByEventIdAndStatus(eq(event.getId()), any());
        verify(statsClient).getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), eq(true));
    }

    @Test
    void shouldGetEventByPublicWhenEventNotFoundThenThrowNotFoundException() {
        when(eventRepository.getEventByPublic(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.getEventByPublic(99L));

        verify(eventRepository).getEventByPublic(99L);
        verify(requestRepository, never()).countByEventIdAndStatus(anyLong(), any());
        verify(statsClient, never()).getStats(any(), any(), anyList(), anyBoolean());
    }
}
