package ru.practicum.user;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);
}
