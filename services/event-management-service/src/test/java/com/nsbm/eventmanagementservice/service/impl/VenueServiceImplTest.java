package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.VenueRequest;
import com.nsbm.eventmanagementservice.dto.VenueResponse;
import com.nsbm.eventmanagementservice.exception.VenueNotFoundException;
import com.nsbm.eventmanagementservice.mapper.VenueMapper;
import com.nsbm.eventmanagementservice.model.Venue;
import com.nsbm.eventmanagementservice.model.VenueType;
import com.nsbm.eventmanagementservice.repository.VenueRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue venue;
    private VenueResponse venueResponse;

    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(1L)
                .name("Auditorium A")
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();

        venueResponse = VenueResponse.builder()
                .id(1L)
                .name("Auditorium A")
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();
    }

    @Test
    void createVenue_savesAndReturnsResponse() {
        VenueRequest request = VenueRequest.builder()
                .name("Auditorium A")
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();

        when(venueMapper.toEntity(request)).thenReturn(venue);
        when(venueRepository.save(venue)).thenReturn(venue);
        when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

        VenueResponse result = venueService.createVenue(request);

        assertThat(result.getName()).isEqualTo("Auditorium A");
        verify(venueRepository).save(venue);
    }

    @Test
    void getAllVenues_returnsMappedList() {
        when(venueRepository.findAll()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

        List<VenueResponse> result = venueService.getAllVenues();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Auditorium A");
    }

    @Test
    void getVenueById_notFound_throwsException() {
        when(venueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.getVenueById(999L))
                .isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void updateVenue_appliesChangesAndSaves() {
        VenueRequest request = VenueRequest.builder()
                .name("Auditorium B")
                .capacity(300)
                .venueType(VenueType.HYBRID)
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(venueRepository.save(venue)).thenReturn(venue);
        when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

        venueService.updateVenue(1L, request);

        verify(venueMapper).updateEntityFromRequest(request, venue);
        verify(venueRepository).save(venue);
    }

    @Test
    void deleteVenue_whenExists_deletes() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));

        venueService.deleteVenue(1L);

        verify(venueRepository).delete(venue);
    }
}
