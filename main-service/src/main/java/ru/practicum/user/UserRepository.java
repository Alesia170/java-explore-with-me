package ru.practicum.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @Query(
            value = """
                    SELECT *
                    FROM users
                    ORDER BY id
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<User> findAllWithOffset(@Param("from") int from,
                                 @Param("size") int size);

    @Query(
            value = """
                    SELECT *
                    FROM users
                    WHERE id IN (:ids)
                    ORDER BY id
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<User> findAllByIdsWithOffset(@Param("ids") List<Long> ids,
                                      @Param("from") int from,
                                      @Param("size") int size);
}
