package ru.practicum.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.event.AdminEventParams;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.State;
import ru.practicum.dto.event.update.UpdateEventAdminRequest;
import ru.practicum.event.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void shouldReturn200WhenGetEventsByAdminWithValidParams() throws Exception {
        when(eventService.getEventsByAdmin(any(AdminEventParams.class)))
                .thenReturn(List.of(new EventFullDto()));

        mockMvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", State.PUBLISHED.name())
                        .param("categories", "1", "2")
                        .param("rangeStart", "2026-05-10 10:00:00")
                        .param("rangeEnd", "2026-05-11 10:00:00")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenGetEventsByAdminWithInvalidFrom() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetEventsByAdminWithInvalidSize() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetEventsByAdminWithInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", "time")
                        .param("rangeEnd", "2026-05-11 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenUpdateEventByAdminWithValidRequest() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setAnnotation("New annotation annotation");
        request.setTitle("New title title");

        when(eventService.updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(new EventFullDto());

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenUpdateEventByAdminWithInvalidEventId() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("New title");

        mockMvc.perform(patch("/admin/events/{eventId}", 0L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUpdateEventByAdminAndEventNotFound() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("New title");

        when(eventService.updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenUpdateEventByAdminHasConflict() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("New title");

        when(eventService.updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ConflictException("Нельзя изменить событие"));

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}