package com.dcm.backend.demo.controller;



import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//register, heartbeat
@RestController
public class WorkerController {

    private final WorkerRepository workerRepository;

    public WorkerController(WorkerRepository workerRepository, JobRepository jobRepository) {
        this.workerRepository = workerRepository;
    }

    private Map<String, Long> workerLastSeen = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public String register(@RequestBody WorkerInfo workerInfo) {

        WorkerInfo worker = workerRepository.findById(workerInfo.workerId).orElse(new WorkerInfo());
        worker.workerId = workerInfo.workerId;
        worker.cpuCores = workerInfo.cpuCores;
        worker.memoryMB = workerInfo.memoryMB;
        worker.os = workerInfo.os;
        worker.hasGpu = workerInfo.hasGpu;
        worker.lastSeen = System.currentTimeMillis(); // persisted here, not on every heartbeat
        workerRepository.save(worker);
        // Re-seed into in-memory map so heartbeat detection works immediately
        workerLastSeen.put(worker.workerId, worker.lastSeen);
        System.out.println("Worker registered: " + worker.workerId +
                " | CPU: " + worker.cpuCores +
                " | RAM: " + worker.memoryMB +
                " | OS: " + worker.os +
                " | GPU: " + worker.hasGpu);

        return "ok";
    }

    @PostMapping("/workers/heartbeat")
    public String heartbeat(@RequestBody Map<String, String> data) {

        String workerId = data.get("workerId");
        workerLastSeen.put(workerId, System.currentTimeMillis()); // fast, in-memory only

        System.out.println(
                "Heartbeat from " + data.get("workerId") +
                        " status=" + data.get("status")
        );
        return "ok";
    }



}
