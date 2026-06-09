package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.WorkerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<WorkerInfo, String> {
}
