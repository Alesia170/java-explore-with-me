package ru.practicum.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM comments
                    WHERE event_id = :eventId
                    ORDER BY created DESC
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Comment> getCommentsByEvent(@Param("eventId") Long eventId,
                                     @Param("from") int from,
                                     @Param("size") int size);

    @Query(
            value = """
                    SELECT *
                    FROM comments
                    WHERE author_id = :userId
                    ORDER BY created DESC
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Comment> getUserComments(@Param("userId") Long userId,
                                  @Param("from") int from,
                                  @Param("size") int size);
}
