package com.portal.platformservice.repository;

import com.portal.platformservice.entity.SystemBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemBannerRepository extends JpaRepository<SystemBanner, UUID> {

    @Query("SELECT b FROM SystemBanner b WHERE b.active = true AND " +
           "(b.startDate IS NULL OR b.startDate <= :today) AND " +
           "(b.endDate IS NULL OR b.endDate >= :today) " +
           "ORDER BY b.createdAt DESC")
    List<SystemBanner> findActiveBanners(LocalDate today);
}
