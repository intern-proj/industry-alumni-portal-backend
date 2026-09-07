package com.portal.user_service.repository;

import com.portal.user_service.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {
    List<Skill> findByUserId(String userId);
    void deleteByUserIdAndSkillId(String userId, String skillId);
}
