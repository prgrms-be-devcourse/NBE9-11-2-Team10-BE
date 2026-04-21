package com.team10.backend.domain.image.service;

import com.team10.backend.domain.image.dto.ImageUploadResponse;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImageUploadServiceTest {

    private S3Client s3Client;
    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        imageUploadService = new ImageUploadService(s3Client);
        ReflectionTestUtils.setField(imageUploadService, "bucket", "team10-images-dev-test");
        ReflectionTestUtils.setField(imageUploadService, "region", "ap-northeast-2");
    }

    @Test
    void uploadImageReturnsS3Url() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cake.jpg",
                "image/jpeg",
                "test-image".getBytes()
        );

        ImageUploadResponse response = imageUploadService.upload(file, "products");

        assertThat(response.imageUrl())
                .startsWith("https://team10-images-dev-test.s3.ap-northeast-2.amazonaws.com/products/")
                .endsWith(".jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void uploadRejectsNonImageFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "memo.txt",
                "text/plain",
                "not-image".getBytes()
        );

        assertThatThrownBy(() -> imageUploadService.upload(file, "products"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteImageDeletesS3Object() {
        String imageUrl = "https://team10-images-dev-test.s3.ap-northeast-2.amazonaws.com/feeds/test-image.jpg";

        imageUploadService.delete(imageUrl);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteRejectsExternalImageUrl() {
        String imageUrl = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/feeds/test-image.jpg";

        assertThatThrownBy(() -> imageUploadService.delete(imageUrl))
                .isInstanceOf(BusinessException.class);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteIfManagedSkipsExternalImageUrl() {
        String imageUrl = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/feeds/test-image.jpg";

        imageUploadService.deleteIfManaged(imageUrl);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }
}
