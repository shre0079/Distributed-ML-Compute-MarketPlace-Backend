package com.dcm.backend.demo.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    // userId -> timestamp (ms) of their most recent logout.
    // Any token issued before this timestamp is treated as invalid.
    //
    // In-memory by design, matching the pattern already used for worker
    // heartbeats and rate limiting in this codebase. Tradeoff: does not
    // survive a backend restart — acceptable for a single-instance
    // deployment. Revisit with Redis if this ever runs multi-instance.
    private final Map<String, Long> revokedAt = new ConcurrentHashMap<>();

    public void revoke(String userId) {
        revokedAt.put(userId, System.currentTimeMillis());
    }

    public boolean isRevoked(String userId, long tokenIssuedAtMillis) {
        Long revokedTimestamp = revokedAt.get(userId);
        return revokedTimestamp != null && tokenIssuedAtMillis < revokedTimestamp;
    }
}