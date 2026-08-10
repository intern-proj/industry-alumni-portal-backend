package com.nsbm.eventmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_speakers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSpeaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id", nullable = false)
    private GuestSpeaker speaker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeakerRole role;
}
