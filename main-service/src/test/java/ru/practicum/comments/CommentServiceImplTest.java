package ru.practicum.comments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.comment.Comment;
import ru.practicum.comment.CommentRepository;
import ru.practicum.comment.CommentServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;
    private User anotherUser;
    private Event event;
    private Event anotherEvent;
    private Comment comment;
    private NewCommentDto newCommentDto;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setName("Author");
        author.setEmail("author@email.com");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setName("Another user");
        anotherUser.setEmail("another@email.com");

        event = new Event();
        event.setId(10L);
        event.setState(State.PUBLISHED);
        event.setComments(0L);

        anotherEvent = new Event();
        anotherEvent.setId(20L);
        anotherEvent.setState(State.PUBLISHED);
        anotherEvent.setComments(0L);

        comment = new Comment();
        comment.setId(100L);
        comment.setText("Старый комментарий");
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now());

        newCommentDto = new NewCommentDto();
        newCommentDto.setText("Новый комментарий");
    }

    @Test
    void createCommentWhenEventPublishedThenSaveComment() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(commentRepository.saveAndFlush(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment savedComment = invocation.getArgument(0);
                    savedComment.setId(101L);
                    return savedComment;
                });

        CommentDto result = commentService.createComment(author.getId(), event.getId(), newCommentDto);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(newCommentDto.getText(), result.getText());
        assertEquals(1L, event.getComments());

        verify(eventRepository).findById(event.getId());
        verify(userRepository).findById(author.getId());
        verify(commentRepository).saveAndFlush(any(Comment.class));
        verify(eventRepository).save(event);
    }

    @Test
    void createCommentWhenEventNotPublishedThenThrowConflictException() {
        event.setState(State.PENDING);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> commentService.createComment(author.getId(), event.getId(), newCommentDto)
        );

        assertEquals("Нельзя комментировать события, которые еще не опубликованы", exception.getMessage());

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void createCommentWhenEventNotFoundThenThrowNotFoundException() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.createComment(author.getId(), event.getId(), newCommentDto)
        );

        assertEquals("Событие с id = " + event.getId() + " не существует", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void createCommentWhenUserNotFoundThenThrowNotFoundException() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(author.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.createComment(author.getId(), event.getId(), newCommentDto)
        );

        assertEquals("Пользователь с id = " + author.getId() + " не найден", exception.getMessage());

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenValidRequestThenUpdateComment() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(commentRepository.saveAndFlush(comment)).thenReturn(comment);

        CommentDto result = commentService.updateComment(
                comment.getId(),
                author.getId(),
                event.getId(),
                newCommentDto
        );

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(newCommentDto.getText(), result.getText());
        assertEquals(newCommentDto.getText(), comment.getText());
        assertNotNull(comment.getUpdated());

        verify(commentRepository).saveAndFlush(comment);
    }

    @Test
    void updateCommentWhenTextIsSameThenReturnCurrentCommentWithoutSaving() {
        newCommentDto.setText(comment.getText());

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        CommentDto result = commentService.updateComment(
                comment.getId(),
                author.getId(),
                event.getId(),
                newCommentDto
        );

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getText(), result.getText());
        assertNull(comment.getUpdated());

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenCommentNotFoundThenThrowNotFoundException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.updateComment(
                        comment.getId(),
                        author.getId(),
                        event.getId(),
                        newCommentDto
                )
        );

        assertEquals("Комментарий с id = " + comment.getId() + " не существует", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(eventRepository, never()).findById(anyLong());
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenUserNotFoundThenThrowNotFoundException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(author.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.updateComment(
                        comment.getId(),
                        author.getId(),
                        event.getId(),
                        newCommentDto
                )
        );

        assertEquals("Пользователь с id = " + author.getId() + " не найден", exception.getMessage());

        verify(eventRepository, never()).findById(anyLong());
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenEventNotFoundThenThrowNotFoundException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.updateComment(
                        comment.getId(),
                        author.getId(),
                        event.getId(),
                        newCommentDto
                )
        );

        assertEquals("Событие с id = " + event.getId() + " не существует", exception.getMessage());

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenCommentDoesNotBelongToEventThenThrowConflictException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(eventRepository.findById(anotherEvent.getId())).thenReturn(Optional.of(anotherEvent));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> commentService.updateComment(
                        comment.getId(),
                        author.getId(),
                        anotherEvent.getId(),
                        newCommentDto
                )
        );

        assertEquals("Комментарий к этому событию не относится", exception.getMessage());

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void updateCommentWhenUserIsNotAuthorThenThrowForbiddenException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userRepository.findById(anotherUser.getId())).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> commentService.updateComment(
                        comment.getId(),
                        anotherUser.getId(),
                        event.getId(),
                        newCommentDto
                )
        );

        assertEquals(
                "Пользователь c id = " + anotherUser.getId() + " не является автором комментария",
                exception.getMessage()
        );

        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void deleteCommentWhenValidRequestThenDeleteComment() {
        event.setComments(1L);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        commentService.deleteComment(comment.getId(), author.getId(), event.getId());

        assertEquals(0L, event.getComments());

        verify(eventRepository).save(event);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentWhenCommentNotFoundThenThrowNotFoundException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.deleteComment(comment.getId(), author.getId(), event.getId())
        );

        assertEquals("Комментарий с id = " + comment.getId() + " не существует", exception.getMessage());

        verify(eventRepository, never()).findById(anyLong());
        verify(userRepository, never()).findById(anyLong());
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteCommentWhenCommentDoesNotBelongToEventThenThrowConflictException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(eventRepository.findById(anotherEvent.getId())).thenReturn(Optional.of(anotherEvent));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> commentService.deleteComment(comment.getId(), author.getId(), anotherEvent.getId())
        );

        assertEquals("Комментарий к этому событию не относится", exception.getMessage());

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteCommentWhenUserIsNotAuthorThenThrowForbiddenException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(anotherUser.getId())).thenReturn(Optional.of(anotherUser));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> commentService.deleteComment(comment.getId(), anotherUser.getId(), event.getId())
        );

        assertEquals("Пользователь с id = " + anotherUser.getId() + " не является автором комментария", exception.getMessage());

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void getCommentsByEventIdWhenEventPublishedThenReturnComments() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(commentRepository.getCommentsByEvent(event.getId(), 0, 10))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getCommentsByEventId(event.getId(), 0, 10);

        assertEquals(1, result.size());
        assertEquals(comment.getId(), result.getFirst().getId());
        assertEquals(comment.getText(), result.getFirst().getText());

        verify(commentRepository).getCommentsByEvent(event.getId(), 0, 10);
    }

    @Test
    void getCommentsByEventIdWhenEventNotPublishedThenThrowNotFoundException() {
        event.setState(State.PENDING);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.getCommentsByEventId(event.getId(), 0, 10)
        );

        assertEquals("Событие с id = " + event.getId() + " не найдено", exception.getMessage());

        verify(commentRepository, never()).getCommentsByEvent(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getCommentsByEventIdWhenEventNotFoundThenThrowNotFoundException() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.getCommentsByEventId(event.getId(), 0, 10)
        );

        assertEquals("Событие с id = " + event.getId() + " не существует", exception.getMessage());

        verify(commentRepository, never()).getCommentsByEvent(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getUserCommentsWhenUserExistsThenReturnComments() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(commentRepository.getUserComments(author.getId(), 0, 10))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getUserComments(author.getId(), 0, 10);

        assertEquals(1, result.size());
        assertEquals(comment.getId(), result.getFirst().getId());
        assertEquals(comment.getText(), result.getFirst().getText());

        verify(commentRepository).getUserComments(author.getId(), 0, 10);
    }

    @Test
    void getUserCommentsWhenUserNotFoundThenThrowNotFoundException() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.getUserComments(author.getId(), 0, 10)
        );

        assertEquals("Пользователь с id = " + author.getId() + " не найден", exception.getMessage());

        verify(commentRepository, never()).getUserComments(anyLong(), anyInt(), anyInt());
    }

    @Test
    void deleteCommentByAdminWhenCommentExistsThenDeleteComment() {
        event.setComments(1L);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        commentService.deleteCommentByAdmin(comment.getId());

        assertEquals(0L, event.getComments());

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentByAdminWhenCommentNotFoundThenThrowNotFoundException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> commentService.deleteCommentByAdmin(comment.getId())
        );

        assertEquals("Комментарий с id = " + comment.getId() + " не существует", exception.getMessage());

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteCommentWhenCommentsCountIsZeroThenShouldNotBecomeNegative() {

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        commentService.deleteComment(comment.getId(), author.getId(), event.getId());

        assertEquals(0L, event.getComments());

        verify(commentRepository).delete(comment);
        verify(eventRepository).save(event);
    }
}