package com.team10.backend.domain.image.controller;

import com.team10.backend.domain.image.dto.ImageUploadResponse;
import com.team10.backend.domain.image.dto.PresignedUrlRequest;
import com.team10.backend.domain.image.dto.PresignedUrlResponse;
import com.team10.backend.domain.image.service.ImageUploadService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Tag(name = "이미지", description = "이미지 업로드 API")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, params = "file")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이미지 단일 업로드", description = "이미지를 S3에 업로드하고 접근 URL을 반환합니다.")
    public ApiResponse<ImageUploadResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "images") String directory
    ) {
        return ApiResponse.ok(imageUploadService.upload(file, directory));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, params = "files")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이미지 다중 업로드", description = "다중 이미지를 S3에 업로드하고 접근 URL을 반환합니다.")
    public ApiResponse<List<ImageUploadResponse>> uploadMultiple(
            @RequestParam List<MultipartFile> files,
            @RequestParam(defaultValue = "images") String directory
    ) {
        List<ImageUploadResponse> response =
                imageUploadService.uploadMultiple(files, directory);
        
        return ApiResponse.ok(response);
    }

    @PostMapping("/presigned-url")
    @Operation(summary = "Presigned URL 발급", description = "프론트가 S3에 직접 업로드할 수 있는 URL과 저장용 이미지 URL을 반환합니다.")
    public ApiResponse<PresignedUrlResponse> createPresignedUrl(
            @RequestBody @Valid PresignedUrlRequest request
    ) {
        return ApiResponse.ok(imageUploadService.createPresignedUrl(request));
    }

}
