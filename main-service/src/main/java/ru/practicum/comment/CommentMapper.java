package ru.practicum.comment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.user.UserMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentMapper {
    public static CommentDto toCommentDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getEvent().getId(),
                UserMapper.toUserShortDto(comment.getAuthor()),
                comment.getCreated(),
                comment.getUpdated()
        );
    }

    public static Comment toComment(NewCommentDto commentDto) {
        Comment comment = new Comment();

        comment.setText(commentDto.getText());

        return comment;
    }
}
