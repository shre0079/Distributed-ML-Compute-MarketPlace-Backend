package com.dcm.backend.demo.controller;

import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    private final Path datasetDir = Path.of("storage/datasets");

    @PostMapping("/upload")
    public String uploadDataset(@RequestParam("file") MultipartFile file) throws Exception {

        Files.createDirectories(datasetDir);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path filePath = datasetDir.resolve(fileName);

        Files.write(filePath, file.getBytes());

        String fileUrl = "http://localhost:8080/files/" + fileName;

        System.out.println("Dataset uploaded: " + fileName);

        return fileUrl;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws Exception {

        Path filePath = datasetDir.resolve(fileName);

        UrlResource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body((Resource) resource);
    }
}