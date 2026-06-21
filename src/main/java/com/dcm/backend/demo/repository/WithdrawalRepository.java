package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.WithdrawalRequest;
import com.dcm.backend.demo.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<WithdrawalRequest, String> {

    List<WithdrawalRequest> findAllByWorkerIdOrderByRequestedAtDesc(String workerId);
    List<WithdrawalRequest> findAllByStatus(WithdrawalStatus status);
}