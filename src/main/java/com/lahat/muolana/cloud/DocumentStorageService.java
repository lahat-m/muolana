package com.lahat.muolana.cloud;

import com.lahat.muolana.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");

    private final MinioClient minioClient;
    private final String bucket;

    public DocumentStorageService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.bucket = props.bucket();
    }

    public String store(byte[] bytes, String originalFilename) {
        String ext = extension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new StorageException("Only PDF and DOCX files are allowed, got: ." + ext);
        String objectKey = "documents/" + UUID.randomUUID() + "_" + sanitize(originalFilename);
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, bytes.length, -1)
                    .contentType(contentType(ext))
                    .build());
            log.info("Stored document in MinIO: {}", objectKey);
            return objectKey;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Failed to store document: " + originalFilename, e);
        }
    }

    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to download document: " + objectKey, e);
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("Deleted document from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.warn("Could not delete document from MinIO: {}", objectKey, e);
        }
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private static String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String contentType(String ext) {
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default     -> "application/octet-stream";
        };
    }
}
