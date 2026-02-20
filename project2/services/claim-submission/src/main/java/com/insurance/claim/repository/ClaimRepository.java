package com.insurance.claim.repository;

import com.insurance.claim.model.ClaimRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Simple in-memory store — no database needed for the demo
@Repository
public class ClaimRepository {

    private final ConcurrentHashMap<String, ClaimRequest> store = new ConcurrentHashMap<>();

    public void save(String id, ClaimRequest request) {
        store.put(id, request);
    }

    public Optional<ClaimRequest> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Map.Entry<String, ClaimRequest>> findAll() {
        return List.copyOf(store.entrySet());
    }
}
