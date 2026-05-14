package ru.practicum.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.request.RequestStatus;
import ru.practicum.dto.event.update.UpdateEventUserRequest;
import ru.practicum.event.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventController.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void shouldReturn200WhenGetEventsByUserWithValidParams() throws Exception {
        when(eventService.getEventsByUser(1L, 0, 10))
                .thenReturn(List.of(new EventFullDto()));

        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenGetEventsByUserWithInvalidUserId() throws Exception {
        mockMvc.perform(get("/users/{userId}/events", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetEventsByUserWithInvalidSize() throws Exception {
        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn201WhenCreateNewEventWithValidRequest() throws Exception {
        String json = "{"
                      + "\"annotation\":\"Краткое описание события\","
                      + "\"category\":1,"
                      + "\"description\":\"Полное описание события\","
                      + "\"eventDate\":\"2026-06-10 10:00:00\","
                      + "\"location\":{"
                      + "\"lat\":55.75,"
                      + "\"lon\":37.61"
                      + "},"
                      + "\"paid\":false,"
                      + "\"participantLimit\":10,"
                      + "\"requestModeration\":true,"
                      + "\"title\":\"Новое событие\""
                      + "}";

        when(eventService.createNewEvent(eq(1L), any(NewEventDto.class)))
                .thenReturn(new EventFullDto());

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn400WhenCreateNewEventWithInvalidUserId() throws Exception {
        String json = "{"
                      + "\"annotation\":\"Краткое описание события\","
                      + "\"category\":1,"
                      + "\"description\":\"Полное описание события\","
                      + "\"eventDate\":\"2026-06-10 10:00:00\","
                      + "\"location\":{"
                      + "\"lat\":55.75,"
                      + "\"lon\":37.61"
                      + "},"
                      + "\"paid\":false,"
                      + "\"participantLimit\":10,"
                      + "\"requestModeration\":true,"
                      + "\"title\":\"Новое событие\""
                      + "}";

        mockMvc.perform(post("/users/{userId}/events", 0L)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreateNewEventWithInvalidBody() throws Exception {
        String json = "{"
                      + "\"annotation\":\"\","
                      + "\"category\":1,"
                      + "\"description\":\"\","
                      + "\"eventDate\":\"2026-06-10 10:00:00\","
                      + "\"title\":\"\""
                      + "}";

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenCreateNewEventAndUserNotFound() throws Exception {
        String json = "{"
                      + "\"annotation\":\"Краткое описание события\","
                      + "\"category\":1,"
                      + "\"description\":\"Полное описание события\","
                      + "\"eventDate\":\"2026-06-10 10:00:00\","
                      + "\"location\":{"
                      + "\"lat\":55.75,"
                      + "\"lon\":37.61"
                      + "},"
                      + "\"paid\":false,"
                      + "\"participantLimit\":10,"
                      + "\"requestModeration\":true,"
                      + "\"title\":\"Новое событие\""
                      + "}";

        when(eventService.createNewEvent(eq(1L), any(NewEventDto.class)))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WhenGetEventByUserWithValidIds() throws Exception {
        when(eventService.getEventByUser(1L, 2L))
                .thenReturn(new EventFullDto());

        mockMvc.perform(get("/users/{userId}/events/{eventId}", 1L, 2L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenGetEventByUserAndEventNotFound() throws Exception {
        when(eventService.getEventByUser(1L, 2L))
                .thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", 1L, 2L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WhenUpdateEventWithValidRequest() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title");
        request.setAnnotation("Updated annotation example");

        when(eventService.updateEvent(eq(1L), eq(2L), any(UpdateEventUserRequest.class)))
                .thenReturn(new EventFullDto());

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenUpdateEventWithInvalidEventId() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title");

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 0L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUpdateEventAndEventNotFound() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title");

        when(eventService.updateEvent(eq(1L), eq(2L), any(UpdateEventUserRequest.class)))
                .thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenUpdateEventHasConflict() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title");

        when(eventService.updateEvent(eq(1L), eq(2L), any(UpdateEventUserRequest.class)))
                .thenThrow(new ConflictException("Нельзя изменить опубликованное событие"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn200WhenGetRequestsByEventWithValidIds() throws Exception {
        when(eventService.getRequestsByEvent(1L, 2L))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", 1L, 2L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenGetRequestsByEventAndEventNotFound() throws Exception {
        when(eventService.getRequestsByEvent(1L, 2L))
                .thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", 1L, 2L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WhenUpdateRequestStatusWithValidRequest() throws Exception {
        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(1L, 2L));
        request.setStatus(RequestStatus.valueOf("CONFIRMED"));

        when(eventService.updateRequestStatus(eq(1L), eq(2L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(new EventRequestStatusUpdateResult());

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenUpdateRequestStatusWithInvalidUserId() throws Exception {
        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(1L));
        request.setStatus(RequestStatus.valueOf("CONFIRMED"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 0L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenUpdateRequestStatusHasConflict() throws Exception {
        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(1L));
        request.setStatus(RequestStatus.valueOf("CONFIRMED"));

        when(eventService.updateRequestStatus(eq(1L), eq(2L), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException("Нельзя изменить статус заявки"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}