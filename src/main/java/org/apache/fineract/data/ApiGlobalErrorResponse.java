package org.apache.fineract.data;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiGlobalErrorResponse {

    private String error;
    private String message;
    private Integer status;

    public static ApiGlobalErrorResponse unauthorized(String message) {
        return new ApiGlobalErrorResponse("Unauthorized", message, 401);
    }

    public static ApiGlobalErrorResponse forbidden(String message) {
        return new ApiGlobalErrorResponse("Forbidden", message, 403);
    }
}