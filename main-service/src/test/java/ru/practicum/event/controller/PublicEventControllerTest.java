package ru.practicum.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.StatsClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventSort;
import ru.practicum.dto.event.PublicEventsParams;
import ru.practicum.event.EventService;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatsClient statsClient;

    @Test
    void shouldReturn200WhenGetEventsByPublicWithValidParams() throws Exception {
        when(eventService.getEventsByPublic(any(PublicEventsParams.class))).thenReturn(List.of(new EventShortDto()));

        mockMvc.perform(get("/events")
                        .param("text", "concert")
                        .param("categories", "1", "2")
                        .param("paid", "false")
                        .param("rangeStart", "2026-05-10 10:00:00")
                        .param("rangeEnd", "2026-05-11 10:00:00")
                        .param("onlyAvailable", "true")
                        .param("sort", EventSort.EVENT_DATE.name())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200WhenGetEventsByPublicAndStatsClientThrowsException() throws Exception {
        doThrow(new RuntimeException("Stats service unavailable"))
                .when(statsClient).create(anyString(), anyString());

        when(eventService.getEventsByPublic(any(PublicEventsParams.class))).thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenGetEventsByPublicWithInvalidFrom() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetEventsByPublicWithInvalidSize() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetEventsByPublicWithInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/events")
                        .param("rangeStart", "time")
                        .param("rangeEnd", "2026-05-11 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenGetEventByPublicWithValidId() throws Exception {
        when(eventService.getEventByPublic(1L))
                .thenReturn(new EventFullDto());

        mockMvc.perform(get("/events/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenGetEventByPublicWithInvalidId() throws Exception {
        mockMvc.perform(get("/events/{id}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenGetEventByPublicAndEventNotFound() throws Exception {
        when(eventService.getEventByPublic(1L))
                .thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(get("/events/{id}", 1L))
                .andExpect(status().isNotFound());
    }
}