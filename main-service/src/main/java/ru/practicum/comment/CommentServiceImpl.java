package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.event.State;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        Event event = getEventOrThrow(eventId);
        User author = getUserOrThrow(userId);

        if (event.getState() != State.PUBLISHED) {
            log.warn("Попытка прокомментировать неопубликованное событие: eventId = {}, userId = {}, state = {}",
                    eventId, userId, event.getState());
            throw new ConflictException("Нельзя комментировать события, которые еще не опубликованы");
        }

        Comment comment = CommentMapper.toComment(dto);
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        Comment savedComment = commentRepository.saveAndFlush(comment);

        event.setComments(event.getComments() + 1);
        eventRepository.save(event);

        log.info("Комментарий успешно сохранен: commentId = {}, eventId = {}, userId = {}",
                savedComment.getId(), eventId, userId);

        return CommentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long commentId, Long userId, Long eventId, NewCommentDto dto) {
        Comment comment = getCommentOrThrow(commentId);
        getUserOrThrow(userId);
        getEventOrThrow(eventId);

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Комментарий к этому событию не относится");
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            log.warn("Попытка редактировать чужой комментарий: commentId = {}, userId = {}, authorId = {}",
                    commentId, userId, comment.getAuthor().getId());
            throw new ForbiddenException("Пользователь c id = " + userId + " не является автором комментария");
        }

        if (comment.getText().equals(dto.getText())) {
            log.info("Текст комментария не изменился");
            return CommentMapper.toCommentDto(comment);
        }

        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        Comment updatedComment = commentRepository.saveAndFlush(comment);

        log.info("Комментарий успешно обновлен: commentId = {}, eventId = {}, userId = {}",
                updatedComment.getId(), eventId, userId);

        return CommentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId, Long eventId) {
        Comment comment = getCommentOrThrow(commentId);
        Event event = getEventOrThrow(eventId);
        getUserOrThrow(userId);

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Комментарий к этому событию не относится");
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            log.warn("Попытка удалить чужой комментарий: commentId = {}, userId = {}, authorId = {}",
                    commentId, userId, comment.getAuthor().getId());
            throw new ForbiddenException("Пользователь с id = " + userId + " не является автором комментария");
        }

        commentRepository.delete(comment);

        event.setComments(Math.max(0L, event.getComments() - 1));
        eventRepository.save(event);

        log.info("Комментарий удален: commentId = {}, eventId = {}, userId = {}", commentId, eventId, userId);
    }

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId, int from, int size) {
        Event event = getEventOrThrow(eventId);

        if (event.getState() != State.PUBLISHED) {
            log.warn("Попытка получить комментарии на событие, которое еще не опубликовано");
            throw new NotFoundException("Событие с id = " + eventId + " не найдено");
        }

        List<Comment> comments = commentRepository.getCommentsByEvent(eventId, from, size);

        log.info("Получено {} комментарий события с id = {}", comments.size(), eventId);

        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        getUserOrThrow(userId);

        List<Comment> comments = commentRepository.getUserComments(userId, from, size);

        log.info("Получено {} комментариев пользователя с id = {}", comments.size(), userId);

        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getCommentOrThrow(commentId);
        Event event = comment.getEvent();

        commentRepository.delete(comment);

        event.setComments(Math.max(0L, event.getComments() - 1));
        eventRepository.save(event);

        log.info("Комментарий удален админом: commentId = {}, eventId = {}", commentId, event.getId());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующего пользователя");
                    return new NotFoundException("Пользователь с id = " + userId + " не найден");
                });
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующее событие");
                    return new NotFoundException("Событие с id = " + eventId + " не существует");
                });
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка найти несуществующий комментарий");
                    return new NotFoundException("Комментарий с id = " + commentId + " не существует");
                });
    }
}
