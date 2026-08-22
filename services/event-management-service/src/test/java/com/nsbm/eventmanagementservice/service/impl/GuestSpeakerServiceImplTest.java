package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.exception.GuestSpeakerNotFoundException;
import com.nsbm.eventmanagementservice.mapper.GuestSpeakerMapper;
import com.nsbm.eventmanagementservice.model.GuestSpeaker;
import com.nsbm.eventmanagementservice.repository.GuestSpeakerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuestSpeakerServiceImplTest {

    @Mock
    private GuestSpeakerRepository guestSpeakerRepository;

    @Mock
    private GuestSpeakerMapper guestSpeakerMapper;

    @InjectMocks
    private GuestSpeakerServiceImpl guestSpeakerService;

    private GuestSpeaker speaker;
    private GuestSpeakerResponse speakerResponse;

    @BeforeEach
    void setUp() {
        speaker = GuestSpeaker.builder()
                .id(1L)
                .fullName("Dr. Nimal Perera")
                .email("nimal.perera@example.com")
                .organizationId(5L)
                .build();

        speakerResponse = GuestSpeakerResponse.builder()
                .id(1L)
                .fullName("Dr. Nimal Perera")
                .email("nimal.perera@example.com")
                .organizationId(5L)
                .build();
    }

    @Test
    void createSpeaker_savesAndReturnsResponse() {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder()
                .fullName("Dr. Nimal Perera")
                .email("nimal.perera@example.com")
                .organizationId(5L)
                .build();

        when(guestSpeakerMapper.toEntity(request)).thenReturn(speaker);
        when(guestSpeakerRepository.save(speaker)).thenReturn(speaker);
        when(guestSpeakerMapper.toResponse(speaker)).thenReturn(speakerResponse);

        GuestSpeakerResponse result = guestSpeakerService.createSpeaker(request);

        assertThat(result.getFullName()).isEqualTo("Dr. Nimal Perera");
        verify(guestSpeakerRepository).save(speaker);
    }

    @Test
    void getSpeakerById_whenExists_returnsSpeaker() {
        when(guestSpeakerRepository.findById(1L)).thenReturn(Optional.of(speaker));
        when(guestSpeakerMapper.toResponse(speaker)).thenReturn(speakerResponse);

        GuestSpeakerResponse result = guestSpeakerService.getSpeakerById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getSpeakerById_whenNotFound_throwsException() {
        when(guestSpeakerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestSpeakerService.getSpeakerById(999L))
                .isInstanceOf(GuestSpeakerNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getAllSpeakers_returnsMappedList() {
        when(guestSpeakerRepository.findAll()).thenReturn(List.of(speaker));
        when(guestSpeakerMapper.toResponse(speaker)).thenReturn(speakerResponse);

        List<GuestSpeakerResponse> result = guestSpeakerService.getAllSpeakers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Dr. Nimal Perera");
    }

    @Test
    void getSpeakersByOrganization_returnsFilteredList() {
        when(guestSpeakerRepository.findByOrganizationId(5L)).thenReturn(List.of(speaker));
        when(guestSpeakerMapper.toResponse(speaker)).thenReturn(speakerResponse);

        List<GuestSpeakerResponse> result = guestSpeakerService.getSpeakersByOrganization(5L);

        assertThat(result).hasSize(1);
        verify(guestSpeakerRepository).findByOrganizationId(5L);
    }

    @Test
    void updateSpeaker_appliesChangesAndSaves() {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder()
                .fullName("Dr. Nimal Perera Jr.")
                .build();

        when(guestSpeakerRepository.findById(1L)).thenReturn(Optional.of(speaker));
        when(guestSpeakerRepository.save(speaker)).thenReturn(speaker);
        when(guestSpeakerMapper.toResponse(speaker)).thenReturn(speakerResponse);

        guestSpeakerService.updateSpeaker(1L, request);

        verify(guestSpeakerMapper).updateEntityFromRequest(request, speaker);
        verify(guestSpeakerRepository).save(speaker);
    }

    @Test
    void updateSpeaker_whenNotFound_throwsException() {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder().fullName("Someone").build();

        when(guestSpeakerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestSpeakerService.updateSpeaker(999L, request))
                .isInstanceOf(GuestSpeakerNotFoundException.class);

        verify(guestSpeakerRepository, never()).save(any());
    }

    @Test
    void deleteSpeaker_whenExists_deletes() {
        when(guestSpeakerRepository.findById(1L)).thenReturn(Optional.of(speaker));

        guestSpeakerService.deleteSpeaker(1L);

        verify(guestSpeakerRepository).delete(speaker);
    }

    @Test
    void deleteSpeaker_whenNotFound_throwsException() {
        when(guestSpeakerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestSpeakerService.deleteSpeaker(999L))
                .isInstanceOf(GuestSpeakerNotFoundException.class);

        verify(guestSpeakerRepository, never()).delete(any());
    }
}
