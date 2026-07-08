package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.service.RateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${app.base-url}")
    private String baseUrl;

    private RateLimitService rateLimitService;

    private final Path datasetDir = Path.of("storage/datasets");

//    @PostMapping("/upload")
//    public String uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
//
//        Files.createDirectories(datasetDir);
//
//        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//        Path destination = datasetDir.resolve(fileName);
//        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
//
//        String fileUrl = baseUrl + "/files/" + fileName;
//        return fileUrl;
//    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws Exception {

        Path filePath = datasetDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping("/files/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!rateLimitService.resolveFileUploadBucket(userId).tryConsume(1)) {
            throw new RateLimitException("Too many uploads. Try again in a minute.");
        }

        Files.createDirectories(datasetDir);
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = datasetDir.resolve(fileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return baseUrl + "/files/" + fileName;
    }
}

