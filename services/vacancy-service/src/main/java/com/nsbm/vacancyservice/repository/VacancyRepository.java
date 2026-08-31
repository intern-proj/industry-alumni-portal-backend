package com.nsbm.vacancyservice.repository;

import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.Vacancy;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

        Page<Vacancy> findByStatus(VacancyStatus status, Pageable pageable);

        Page<Vacancy> findByStatusAndJobType(VacancyStatus status, JobType jobType, Pageable pageable);

        Page<Vacancy> findByPartnerId(String partnerId, Pageable pageable);

        Page<Vacancy> findByPartnerIdAndStatus(String partnerId, VacancyStatus status, Pageable pageable);

        long countByStatus(VacancyStatus status);

        long countByPartnerId(String partnerId);

        @Query("SELECT v FROM Vacancy v WHERE v.status = :status " +
                        "AND (LOWER(v.title) LIKE :keywordPattern " +
                        "OR LOWER(v.companyName) LIKE :keywordPattern " +
                        "OR LOWER(v.location) LIKE :keywordPattern " +
                        "OR LOWER(v.tags) LIKE :keywordPattern)")
        Page<Vacancy> searchPublicVacanciesWithKeyword(
                        @Param("status") VacancyStatus status,
                        @Param("keywordPattern") String keywordPattern,
                        Pageable pageable);

        @Query("SELECT v FROM Vacancy v WHERE v.status = :status AND v.jobType = :jobType " +
                        "AND (LOWER(v.title) LIKE :keywordPattern " +
                        "OR LOWER(v.companyName) LIKE :keywordPattern " +
                        "OR LOWER(v.location) LIKE :keywordPattern " +
                        "OR LOWER(v.tags) LIKE :keywordPattern)")
        Page<Vacancy> searchPublicVacanciesWithKeywordAndJobType(
                        @Param("status") VacancyStatus status,
                        @Param("jobType") JobType jobType,
                        @Param("keywordPattern") String keywordPattern,
                        Pageable pageable);

        @Query("SELECT v FROM Vacancy v WHERE " +
                        "LOWER(v.title) LIKE :keywordPattern " +
                        "OR LOWER(v.companyName) LIKE :keywordPattern")
        Page<Vacancy> searchAdminVacanciesWithKeyword(
                        @Param("keywordPattern") String keywordPattern,
                        Pageable pageable);

        @Query("SELECT v FROM Vacancy v WHERE v.status = :status AND (" +
                        "LOWER(v.title) LIKE :keywordPattern " +
                        "OR LOWER(v.companyName) LIKE :keywordPattern)")
        Page<Vacancy> searchAdminVacanciesWithStatusAndKeyword(
                        @Param("status") VacancyStatus status,
                        @Param("keywordPattern") String keywordPattern,
                        Pageable pageable);
}
