package ru.practicum.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user1;
    private User user2;
    private NewUserRequest newUserRequest;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setName("name1");
        user1.setEmail("email1@email.com");

        user2 = new User();
        user2.setId(2L);
        user2.setName("name2");
        user2.setEmail("email2@email.com");

        newUserRequest = new NewUserRequest();
        newUserRequest.setName("name3");
        newUserRequest.setEmail("new@mail.com");
    }

    @Test
    void shouldReturnAllUsersWhenIdsEmpty() {
        when(userRepository.findAllWithOffset(0, 10))
                .thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getEmail()).isEqualTo("email1@email.com");
        assertThat(result.getFirst().getName()).isEqualTo("name1");

        verify(userRepository).findAllWithOffset(0, 10);
        verify(userRepository, never()).findAllByIdsWithOffset(anyList(), anyInt(), anyInt());
    }

    @Test
    void shouldReturnUsersByIdsWhenIdsPresent() {
        List<Long> ids = List.of(1L, 2L);

        when(userRepository.findAllByIdsWithOffset(ids, 0, 10))
                .thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.getUsers(ids, 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(userRepository).findAllByIdsWithOffset(ids, 0, 10);
        verify(userRepository, never()).findAllWithOffset(anyInt(), anyInt());
    }

    @Test
    void shouldReturnEmptyListWhenUsersNotFound() {
        when(userRepository.findAllWithOffset(0, 10))
                .thenReturn(List.of());

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertThat(result).isEmpty();

        verify(userRepository).findAllWithOffset(0, 10);
    }

    @Test
    void shouldSaveAndReturnUserDtoWhenEmailIsUnique() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("new@mail.com");
        savedUser.setName("newName");

        when(userRepository.existsByEmail("new@mail.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserDto result = userService.createUser(newUserRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
        assertThat(result.getName()).isEqualTo("newName");

        verify(userRepository).existsByEmail("new@mail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("new@mail.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(newUserRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmail("new@mail.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeleteUserWhenUserExists() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user1));

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(user1);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("was not found");

        verify(userRepository).findById(99L);
        verify(userRepository, never()).delete(any(User.class));
    }
}
