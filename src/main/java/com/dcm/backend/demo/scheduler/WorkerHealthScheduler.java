package com.dcm.backend.demo.scheduler;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkerHealthScheduler {

    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;

    public WorkerHealthScheduler(JobRepository jobRepository, WorkerRepository workerRepository) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
    }

    private Map<String, Long> workerLastSeen = new ConcurrentHashMap<>();

    @PostConstruct
    public void recoverDeadWorkersOnStartup() {

        long now = System.currentTimeMillis();
        List<WorkerInfo> allWorkers = workerRepository.findAll();

        for (WorkerInfo worker : allWorkers) {
            if (now - worker.lastSeen > 15000) {
                System.out.println("Startup: worker " + worker.workerId + " was dead, requeuing jobs.");
                List<Job> stuckJobs = jobRepository.findAllByWorkerIdAndStatus(
                        worker.workerId, JobStatus.RUNNING);
                for (Job job : stuckJobs) {
                    job.status = JobStatus.CREATED;
                    job.workerId = null;
                    jobRepository.save(job);
                    System.out.println("Startup: requeued job " + job.jobId);
                }
            } else {
                workerLastSeen.put(worker.workerId, worker.lastSeen);
                System.out.println("Startup: re-seeded worker " + worker.workerId);
            }
        }
    }

    // Called by WorkerController on registration
    public void workerRegistered(String workerId, long lastSeen) {
        workerLastSeen.put(workerId, lastSeen);
    }

    // Called by WorkerController on heartbeat
    public void workerHeartbeat(String workerId) {
        workerLastSeen.put(workerId, System.currentTimeMillis());
    }

    @Scheduled(fixedRate = 5000)
    public void checkDeadWorkers() {

        if (workerLastSeen.isEmpty()) return;

        long now = System.currentTimeMillis();

        List<WorkerInfo> allWorkers = workerRepository.findAll();

        for (WorkerInfo worker : allWorkers) {

            if (now - worker.lastSeen > 15000) {
                // Worker was dead before restart, reset their stuck jobs
                System.out.println("Startup: worker " + worker.workerId + " was dead, requeuing jobs.");
                List<Job> stuckJobs = jobRepository.findAllByWorkerIdAndStatus(
                        worker.workerId, JobStatus.RUNNING);
                for (Job job : stuckJobs) {
                    job.status = JobStatus.CREATED;
                    job.workerId = null;
                    jobRepository.save(job);
                    System.out.println("Startup: requeued job " + job.jobId);
                }

            } else {
                // Worker was recently alive before restart, re-seed the in-memory map
                workerLastSeen.put(worker.workerId, worker.lastSeen);
                System.out.println("Startup: re-seeded worker " + worker.workerId + " into heartbeat map.");
            }
        }
    }
}
