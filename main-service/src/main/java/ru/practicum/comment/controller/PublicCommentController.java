package ru.practicum.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.CommentService;
import ru.practicum.dto.comment.CommentDto;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events/{eventId}/comments")
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsByEventId(@PathVariable @Positive Long eventId,
                                                 @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                 @RequestParam(defaultValue = "10") @Positive int size) {

        return commentService.getCommentsByEventId(eventId, from, size);
    }
}
