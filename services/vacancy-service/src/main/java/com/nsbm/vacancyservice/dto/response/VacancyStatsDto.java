package com.nsbm.vacancyservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyStatsDto {

    private long totalVacancies;
    private long approvedVacancies;
    private long pendingVacancies;
    private long rejectedVacancies;
    private long closedVacancies;
}
