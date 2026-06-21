package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.exception.ConflictException;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.exception.UnauthorizedException;
import com.dcm.backend.demo.repository.WorkerRepository;
import com.dcm.backend.demo.scheduler.WorkerHealthScheduler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkerHealthScheduler workerHealthScheduler;

    public WorkerService(WorkerRepository workerRepository,
                         PasswordEncoder passwordEncoder,
                         WorkerHealthScheduler workerHealthScheduler) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.workerHealthScheduler = workerHealthScheduler;
    }

    public String register(WorkerInfo workerInfo) {

        workerRepository.findById(workerInfo.workerId).ifPresent(existing -> {
            // Worker already exists — validate secret matches
            if (!passwordEncoder.matches(workerInfo.workerSecret,
                    existing.workerSecret)) {
                throw new UnauthorizedException(
                        "Worker already registered with a different secret");
            }
        });

        WorkerInfo worker = workerRepository.findById(workerInfo.workerId)
                .orElse(new WorkerInfo());

        worker.workerId = workerInfo.workerId;
        worker.cpuCores = workerInfo.cpuCores;
        worker.memoryMB = workerInfo.memoryMB;
        worker.os = workerInfo.os;
        worker.hasGpu = workerInfo.hasGpu;
        worker.lastSeen = System.currentTimeMillis();

        // Hash secret on first registration, keep existing hash on re-registration
        if (worker.workerSecret == null) {
            worker.workerSecret = passwordEncoder.encode(workerInfo.workerSecret);
        }

        // Rates only set on FIRST registration — later changes go through PUT /workers/rate
        if (isNewWorker) {
            validateRateBounds(workerInfo.cpuRatePerSecond, workerInfo.gpuRatePerSecond);
            worker.cpuRatePerSecond = workerInfo.cpuRatePerSecond;
            worker.gpuRatePerSecond = workerInfo.gpuRatePerSecond;
        }

        workerRepository.save(worker);
        workerHealthScheduler.workerRegistered(worker.workerId, worker.lastSeen);

        System.out.println("Worker registered: " + worker.workerId +
                " | CPU: " + worker.cpuCores +
                " | RAM: " + worker.memoryMB +
                " | OS: " + worker.os +
                " | GPU: " + worker.hasGpu);

        return "ok";
    }

    public void validateWorker(String workerId, String workerSecret) {

        WorkerInfo worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        if (!passwordEncoder.matches(workerSecret, worker.workerSecret)) {
            throw new UnauthorizedException("Invalid worker credentials");
        }
    }

    public void heartbeat(String workerId, String workerSecret, String status) {

        validateWorker(workerId, workerSecret);
        workerHealthScheduler.workerHeartbeat(workerId);

        System.out.println("Heartbeat from " + workerId + " status=" + status);
    }
}