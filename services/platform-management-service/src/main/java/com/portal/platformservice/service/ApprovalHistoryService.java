package com.portal.platformservice.service;

import com.portal.platformservice.dto.response.ApprovalHistoryResponse;
import com.portal.platformservice.entity.ApprovalStatusHistory;
import com.portal.platformservice.entity.ApprovalType;
import com.portal.platformservice.repository.ApprovalStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalHistoryService {

    private final ApprovalStatusHistoryRepository historyRepository;

    @Transactional
    public void record(ApprovalType approvalType, UUID approvalId, String fromStatus,
                        String toStatus, UUID changedByUserId, String remarks) {
        ApprovalStatusHistory entry = ApprovalStatusHistory.builder()
                .approvalType(approvalType)
                .approvalId(approvalId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedByUserId(changedByUserId)
                .remarks(remarks)
                .build();
        historyRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistoryResponse> getHistory(ApprovalType approvalType, UUID approvalId) {
        return historyRepository.findByApprovalTypeAndApprovalIdOrderByChangedAtAsc(approvalType, approvalId)
                .stream()
                .map(entry -> ApprovalHistoryResponse.builder()
                        .fromStatus(entry.getFromStatus())
                        .toStatus(entry.getToStatus())
                        .changedByUserId(entry.getChangedByUserId())
                        .changedAt(entry.getChangedAt())
                        .remarks(entry.getRemarks())
                        .build())
                .toList();
    }
}
