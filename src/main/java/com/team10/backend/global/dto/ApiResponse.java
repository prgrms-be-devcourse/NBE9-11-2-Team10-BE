package com.team10.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success; // 성공, 실패 여부
    private final T data; // 실제 데이터 (상품, 주문 등등)
    private final ErrorInfo error; // 에러 정보

    private ApiResponse(boolean success, T data, ErrorInfo error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // 성공했을 때, 데이터 있음 (조회 메서드에서 많이 쓸 것 같습니다)
    // 예시 { "success": true, "data": { "name": "ex1", "price": 5000 } }
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 성공했을 때, 데이터 없음 (delete?)
    // 예시 { "success": true }
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message));
    }

    @Getter
    public static class ErrorInfo {
        private final String code;
        private final String message;

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
