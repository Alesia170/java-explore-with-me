package ru.practicum.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comment.CommentService;
import ru.practicum.comment.controller.AdminCommentController;
import ru.practicum.comment.controller.PrivateCommentController;
import ru.practicum.comment.controller.PublicCommentController;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminCommentController.class,
        PrivateCommentController.class,
        PublicCommentController.class
})
class CommentControllerTest {

    private static final int DEFAULT_FROM = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    private Long userId;
    private Long eventId;
    private Long commentId;

    private NewCommentDto newCommentDto;
    private CommentDto commentDto;
    private CommentDto secondCommentDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        eventId = 2L;
        commentId = 3L;

        newCommentDto = new NewCommentDto();
        newCommentDto.setText("Комментарий");

        commentDto = new CommentDto();
        commentDto.setId(commentId);
        commentDto.setText("Комментарий");
        commentDto.setCreated(LocalDateTime.now());

        secondCommentDto = new CommentDto();
        secondCommentDto.setId(4L);
        secondCommentDto.setText("Второй комментарий");
        secondCommentDto.setCreated(LocalDateTime.now());
    }

    @Test
    void createCommentWhenValidRequestThenReturnCreatedComment() throws Exception {
        when(commentService.createComment(eq(userId), eq(eventId), any(NewCommentDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value(newCommentDto.getText()));

        verify(commentService).createComment(eq(userId), eq(eventId), any(NewCommentDto.class));
    }

    @Test
    void updateCommentWhenValidRequestThenReturnUpdatedComment() throws Exception {
        newCommentDto.setText("Обновленный комментарий");
        commentDto.setText("Обновленный комментарий");

        when(commentService.updateComment(eq(commentId), eq(userId), eq(eventId), any(NewCommentDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/comments/{commentId}",
                        userId, eventId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Обновленный комментарий"));

        verify(commentService).updateComment(eq(commentId), eq(userId), eq(eventId), any(NewCommentDto.class));
    }

    @Test
    void deleteCommentWhenValidRequestThenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/{userId}/events/{eventId}/comments/{commentId}",
                        userId, eventId, commentId))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(commentId, userId, eventId);
    }

    @Test
    void getUserCommentsWhenValidRequestThenReturnComments() throws Exception {
        when(commentService.getUserComments(userId, DEFAULT_FROM, DEFAULT_SIZE))
                .thenReturn(List.of(commentDto, secondCommentDto));

        mockMvc.perform(get("/users/{userId}/comments", userId)
                        .param("from", String.valueOf(DEFAULT_FROM))
                        .param("size", String.valueOf(DEFAULT_SIZE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].text").value(commentDto.getText()))
                .andExpect(jsonPath("$[1].id").value(secondCommentDto.getId()))
                .andExpect(jsonPath("$[1].text").value(secondCommentDto.getText()));

        verify(commentService).getUserComments(userId, DEFAULT_FROM, DEFAULT_SIZE);
    }

    @Test
    void getUserCommentsWhenParamsNotPassedThenUseDefaultPagination() throws Exception {
        when(commentService.getUserComments(userId, DEFAULT_FROM, DEFAULT_SIZE))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/{userId}/comments", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(commentService).getUserComments(userId, DEFAULT_FROM, DEFAULT_SIZE);
    }

    @Test
    void getCommentsByEventIdWhenValidRequestThenReturnComments() throws Exception {
        when(commentService.getCommentsByEventId(eventId, DEFAULT_FROM, DEFAULT_SIZE))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", String.valueOf(DEFAULT_FROM))
                        .param("size", String.valueOf(DEFAULT_SIZE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].text").value(commentDto.getText()));

        verify(commentService).getCommentsByEventId(eventId, DEFAULT_FROM, DEFAULT_SIZE);
    }

    @Test
    void getCommentsByEventIdWhenParamsNotPassedThenUseDefaultPagination() throws Exception {
        when(commentService.getCommentsByEventId(eventId, DEFAULT_FROM, DEFAULT_SIZE))
                .thenReturn(List.of());

        mockMvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(commentService).getCommentsByEventId(eventId, DEFAULT_FROM, DEFAULT_SIZE);
    }

    @Test
    void deleteCommentByAdminWhenValidRequestThenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());

        verify(commentService).deleteCommentByAdmin(commentId);
    }

    @Test
    void createCommentWhenUserIdIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", -1, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).createComment(anyLong(), anyLong(), any(NewCommentDto.class));
    }

    @Test
    void createCommentWhenEventIdIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", userId, -2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).createComment(anyLong(), anyLong(), any(NewCommentDto.class));
    }

    @Test
    void updateCommentWhenCommentIdIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/users/{userId}/events/{eventId}/comments/{commentId}",
                        userId, eventId, -3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isBadRequest());

        verify(commentService, never())
                .updateComment(anyLong(), anyLong(), anyLong(), any(NewCommentDto.class));
    }

    @Test
    void getUserCommentsWhenFromIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/{userId}/comments", userId)
                        .param("from", "-1")
                        .param("size", String.valueOf(DEFAULT_SIZE)))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).getUserComments(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getUserCommentsWhenSizeIsZeroThenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/{userId}/comments", userId)
                        .param("from", String.valueOf(DEFAULT_FROM))
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).getUserComments(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getCommentsByEventIdWhenFromIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", "-1")
                        .param("size", String.valueOf(DEFAULT_SIZE)))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).getCommentsByEventId(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getCommentsByEventIdWhenSizeIsZeroThenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", String.valueOf(DEFAULT_FROM))
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).getCommentsByEventId(anyLong(), anyInt(), anyInt());
    }

    @Test
    void deleteCommentByAdminWhenCommentIdIsNegativeThenReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/comments/{commentId}", -1))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).deleteCommentByAdmin(anyLong());
    }
}
