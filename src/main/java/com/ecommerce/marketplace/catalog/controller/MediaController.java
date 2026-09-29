package com.ecommerce.marketplace.catalog.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.marketplace.catalog.dto.ImageUploadResponse;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Upload", description = "Endpoints for uploading product images and seller documents")
public class MediaController {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image for products, listings, or seller verification")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {

        if (file.isEmpty()) {
            throw new BusinessRuleException("Cannot upload empty file", "EMPTY_FILE");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx + 1).toLowerCase();
        }

        // Check if Cloudinary is configured with actual credentials
        if (cloudName != null && !cloudName.isBlank() && !cloudName.equalsIgnoreCase("mock")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"
                    )
                );

                String secureUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");
                String format = (String) uploadResult.get("format");
                Long sizeBytes = uploadResult.get("bytes") instanceof Number
                    ? ((Number) uploadResult.get("bytes")).longValue()
                    : file.getSize();

                ImageUploadResponse response = ImageUploadResponse.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .format(format != null ? format : extension)
                    .sizeBytes(sizeBytes)
                    .build();

                return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
            } catch (Exception e) {
                log.warn("Cloudinary upload failed, falling back to local file storage: {}", e.getMessage());
            }
        }

        // Fallback: Local file storage
        try {
            Path targetDir = Paths.get(uploadDir, folder);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String filename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String localUrl = "/uploads/" + folder + "/" + filename;
            ImageUploadResponse response = ImageUploadResponse.builder()
                .url(localUrl)
                .publicId(filename)
                .format(extension)
                .sizeBytes(file.getSize())
                .build();

            return ResponseEntity.ok(ApiResponse.success("File stored locally", response));
        } catch (IOException ioException) {
            log.error("Failed to store file locally: {}", ioException.getMessage(), ioException);
            throw new BusinessRuleException("Failed to upload file: " + ioException.getMessage(), "UPLOAD_FAILED");
        }
    }
}
