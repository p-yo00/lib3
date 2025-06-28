package com.example.demo2.repository;

import com.example.demo.entity.MatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {
    List<MatchRequest> findByMentorId(Long mentorId);
    List<MatchRequest> findByMenteeId(Long menteeId);
}
