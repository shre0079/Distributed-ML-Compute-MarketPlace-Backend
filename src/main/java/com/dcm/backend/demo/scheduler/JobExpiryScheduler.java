package com.dcm.backend.demo.scheduler;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.service.AdminService;
import com.dcm.backend.demo.service.JobService;
import com.dcm.backend.demo.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobExpiryScheduler {

    private final JobRepository jobRepository;
    private final WalletService walletService;

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

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

                // JobExpiryScheduler
                log.warn("Job {} EXPIRED — target worker {} never picked it up", job.jobId, job.targetWorkerId);

            }
        }
    }
}