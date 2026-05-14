package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {

        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAllWithOffset(from, size);
            log.info("Получение пользователей: from = {}, size = {}", from, size);
        } else {
            users = userRepository.findAllByIdsWithOffset(ids, from, size);
            log.info("Получение пользователей по ids: ids = {}, from = {}, siz e= {}", ids, from, size);
        }

        log.info("Выведено количество пользователей = {}", users.size());

        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("User with email=" + newUserRequest.getEmail() + " already exists");
        }

        User user = UserMapper.toUser(newUserRequest);
        User savedUser = userRepository.save(user);

        log.info("Пользователь c id = {} зарегистрирован", savedUser.getId());

        return UserMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        userRepository.delete(user);
        log.info("Пользователь с id = {} удален", userId);
    }
}
