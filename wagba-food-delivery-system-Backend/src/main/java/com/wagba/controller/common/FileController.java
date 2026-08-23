package com.wagba.controller.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "File too large (max 5MB)"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cannot create upload directory"));
        }

        String filename = UUID.randomUUID() + ext;
        File target = new File(dir, filename);
        try {
            file.transferTo(target);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("url", "/uploads/" + filename));
    }
}
