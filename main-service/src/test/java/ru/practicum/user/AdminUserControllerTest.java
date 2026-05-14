package ru.practicum.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void shouldReturn200WhenGetUsersWithValidParams() throws Exception {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setName("Alesia");
        user.setEmail("alesia@example.com");

        when(userService.getUsers(List.of(1L, 2L), 0, 10))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1", "2")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alesia"))
                .andExpect(jsonPath("$[0].email").value("alesia@example.com"));
    }

    @Test
    void shouldReturn200WhenGetUsersWithoutIds() throws Exception {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setName("Alesia");
        user.setEmail("alesia@example.com");

        when(userService.getUsers(null, 0, 10))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alesia"))
                .andExpect(jsonPath("$[0].email").value("alesia@example.com"));
    }

    @Test
    void shouldReturn400WhenGetUsersWithInvalidId() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("ids", "0")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetUsersWithInvalidFrom() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenGetUsersWithInvalidSize() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn201WhenCreateUserWithValidRequest() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("Alesia");
        request.setEmail("alesia@example.com");

        UserDto response = new UserDto();
        response.setId(1L);
        response.setName("Alesia");
        response.setEmail("alesia@example.com");

        when(userService.createUser(any(NewUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alesia"))
                .andExpect(jsonPath("$.email").value("alesia@example.com"));
    }

    @Test
    void shouldReturn400WhenCreateUserWithBlankName() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("");
        request.setEmail("alesia@example.com");

        mockMvc.perform(post("/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreateUserWithInvalidEmail() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("Alesia");
        request.setEmail("invalid-email");

        mockMvc.perform(post("/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenCreateUserWithDuplicateEmail() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("Alesia");
        request.setEmail("alesia@example.com");

        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Пользователь с таким email уже существует"));

        mockMvc.perform(post("/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn204WhenDeleteUserWithValidId() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", 1L))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void shouldReturn400WhenDeleteUserWithInvalidId() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenDeleteUserAndUserNotFound() throws Exception {
        doThrow(new NotFoundException("Пользователь не найден"))
                .when(userService).deleteUser(1L);

        mockMvc.perform(delete("/admin/users/{userId}", 1L))
                .andExpect(status().isNotFound());
    }
}
