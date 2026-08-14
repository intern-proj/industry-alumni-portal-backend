package com.portal.platformservice.repository;

import com.portal.platformservice.entity.ApprovalStatusHistory;
import com.portal.platformservice.entity.ApprovalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalStatusHistoryRepository extends JpaRepository<ApprovalStatusHistory, UUID> {

    List<ApprovalStatusHistory> findByApprovalTypeAndApprovalIdOrderByChangedAtAsc(
            ApprovalType approvalType, UUID approvalId);
}
