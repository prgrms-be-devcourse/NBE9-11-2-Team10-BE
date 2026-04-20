package com.team10.backend.domain.image.service;

import com.team10.backend.domain.image.dto.ImageUploadResponse;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    // 이미지 파일을 검증한 뒤 S3에 업로드하고, DB에 저장할 수 있는 imageUrl을 반환한다.
    public ImageUploadResponse upload(MultipartFile file, String directory) {
        validate(file);

        String key = createObjectKey(file, directory);
        String bucketName = bucket.trim();

        // S3에 저장할 버킷, 객체 key, 파일 타입/크기를 지정한다.
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            // MultipartFile의 실제 바이트 스트림을 S3 object로 업로드한다.
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("S3 image upload failed. bucket={}, key={}, reason={}", bucketName, key, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return new ImageUploadResponse(createImageUrl(key));
    }

    // imageUrl에서 S3 object key를 추출해 실제 S3 파일을 삭제한다.
    public void delete(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }

        String key = extractObjectKey(imageUrl);
        String bucketName = bucket.trim();

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            log.error("S3 image delete failed. bucket={}, key={}, reason={}", bucketName, key, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    // 피드/상품 수정 시 기존 이미지가 우리 S3 URL인 경우에만 삭제한다.
    public void deleteIfManaged(String imageUrl) {
        if (!isManagedImageUrl(imageUrl)) {
            return;
        }

        delete(imageUrl);
    }

    // 버킷 설정, 빈 파일 여부, 허용된 이미지 MIME 타입인지 확인한다.
    private void validate(MultipartFile file) {
        if (!StringUtils.hasText(bucket)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "S3 버킷 설정이 없습니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "업로드할 이미지 파일이 없습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // S3 object key를 만든다. 예: feeds/{uuid}.jpg
    private String createObjectKey(MultipartFile file, String directory) {
        String safeDirectory = sanitizeDirectory(directory);
        String extension = getExtension(file.getOriginalFilename());
        return safeDirectory + "/" + UUID.randomUUID() + extension;
    }

    // directory 값에서 위험하거나 불필요한 문자를 제거해 S3 prefix로 안전하게 사용한다.
    private String sanitizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            return "images";
        }
        String sanitized = directory.replaceAll("[^a-zA-Z0-9/_-]", "");
        sanitized = sanitized.replaceAll("^/+", "").replaceAll("/+$", "");
        return StringUtils.hasText(sanitized) ? sanitized : "images";
    }

    // 원본 파일명에서 확장자만 추출해 UUID 파일명 뒤에 붙인다.
    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String cleanFilename = StringUtils.cleanPath(filename);
        int extensionIndex = cleanFilename.lastIndexOf(".");
        return extensionIndex >= 0 ? cleanFilename.substring(extensionIndex).toLowerCase() : "";
    }

    // 우리 S3 버킷 URL에서만 object key를 뽑아낸다. 예: https://bucket.s3.region.amazonaws.com/feeds/a.jpg -> feeds/a.jpg
    private String extractObjectKey(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            if (!isManagedImageUrl(uri)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "우리 S3 버킷 이미지 URL만 삭제할 수 있습니다.");
            }

            String path = uri.getPath();
            if (!StringUtils.hasText(path) || "/".equals(path)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "삭제할 이미지 경로가 없습니다.");
            }

            return URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 URL 형식이 올바르지 않습니다.");
        }
    }

    private boolean isManagedImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }

        try {
            return isManagedImageUrl(URI.create(imageUrl.trim()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isManagedImageUrl(URI uri) {
        String expectedHost = bucket.trim() + ".s3." + region.trim() + ".amazonaws.com";
        return expectedHost.equals(uri.getHost());
    }

    // S3에 저장된 object key를 기반으로 클라이언트가 사용할 imageUrl을 만든다.
    private String createImageUrl(String key) {
        return "https://" + bucket.trim() + ".s3." + region.trim() + ".amazonaws.com/" + key;
    }
}
