package com.dcm.backend.demo.scheduler;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.service.WalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobExpiryScheduler {

    private final JobRepository jobRepository;
    private final WalletService walletService;

    public JobExpiryScheduler(JobRepository jobRepository, WalletService walletService) {
        this.jobRepository = jobRepository;
        this.walletService = walletService;
    }

    @Scheduled(fixedRate = 30000)
    public void expireUnpickedJobs() {

        long now = System.currentTimeMillis();
        List<Job> createdJobs = jobRepository.findAllByStatus(JobStatus.CREATED);

        for (Job job : createdJobs) {
            if (now > job.expiresAt) {
                job.status = JobStatus.EXPIRED;
                jobRepository.save(job);

                walletService.processJobExpiry(job.jobId);

                System.out.println("Job " + job.jobId + " EXPIRED — target worker "
                        + job.targetWorkerId + " never picked it up");
            }
        }
    }
}