package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, String> {

    void deleteByCandidateIdAndSkillId(String candidateId, String skillId);


    @Modifying
    @Query("delete from CandidateSkill cs where cs.candidate.id = :cid")
    void deleteByCandidateId(@Param("cid") String candidateId);

    @Modifying
    @Query("delete from CandidateSkill cs where cs.candidate.id = :cid and cs.skill.id in :sids")
    void deleteByCandidateIdAndSkillIdIn(@Param("cid") String candidateId,
                                         @Param("sids") Collection<String> skillIds);
}
