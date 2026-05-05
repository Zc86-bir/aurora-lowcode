package com.aurora.core.adapter.web;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal file upload/download controller using Alibaba Cloud OSS.
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileStorageController {

    @Autowired
    private OSS ossClient;

    @Value("${aurora.storage.oss.bucket-name}")
    private String bucketName;

    @Value("${aurora.storage.oss.endpoint}")
    private String endpoint;

    /**
     * Upload a file to OSS. Returns the public URL (bucket is public-read).
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String key = "uploads/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            PutObjectResult result = ossClient.putObject(bucketName, key, file.getInputStream());

            // Build public URL (bucket is public-read)
            String url = "https://" + bucketName + "." + endpoint + "/" + key;

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "key", key,
                    "url", url,
                    "etag", result.getETag()
                )
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Generate a presigned URL for temporary access (useful for private files).
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(@RequestParam String key,
                                              @RequestParam(defaultValue = "3600") int expireSeconds) {
        URL url = ossClient.generatePresignedUrl(
            bucketName,
            key,
            Date.from(Instant.now().plusSeconds(expireSeconds))
        );
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("url", url.toString())));
    }

    /**
     * Delete a file from OSS.
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteFile(@PathVariable String key) {
        ossClient.deleteObject(bucketName, key);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("deleted", key)));
    }
}
