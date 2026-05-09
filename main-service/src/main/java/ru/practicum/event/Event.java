package ru.practicum.event;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.category.Category;
import ru.practicum.dto.event.Location;
import ru.practicum.dto.event.State;
import ru.practicum.user.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "confirmed_requests", nullable = false)
    private Integer confirmedRequests = 0;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration = true;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private Long views = 0L;
}
