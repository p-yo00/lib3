package com.example.demo.service;

import com.example.demo.entity.MatchRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.MatchRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchRequestService {
    @Autowired
    private MatchRequestRepository matchRequestRepository;

    public MatchRequest save(MatchRequest matchRequest) {
        return matchRequestRepository.save(matchRequest);
    }

    public List<MatchRequest> findByMentorId(Long mentorId) {
        return matchRequestRepository.findByMentorId(mentorId);
    }

    public List<MatchRequest> findByMenteeId(Long menteeId) {
        return matchRequestRepository.findByMenteeId(menteeId);
    }
}
