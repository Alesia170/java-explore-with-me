package ru.practicum.comment;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long commentId, Long userId, Long eventId, NewCommentDto dto);

    void deleteComment(Long commentId, Long userId, Long eventId);

    List<CommentDto> getCommentsByEventId(Long eventId, int from, int size);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    void deleteCommentByAdmin(Long commentId);
}
