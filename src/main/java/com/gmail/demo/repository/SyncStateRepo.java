package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.SyncState;

public interface SyncStateRepo extends JpaRepository<SyncState, String> {}
