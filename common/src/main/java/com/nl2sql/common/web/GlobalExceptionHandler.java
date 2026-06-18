package com.nl2sql.common.web;

import com.nl2sql.common.R;
import com.nl2sql.common.enums.ResultCode;
import com.nl2sql.common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转换为 {@code R<T>} 响应。
 * <ul>
 *   <li>{@link BaseException} —— 业务异常，按其携带的错误码/消息返回</li>
 *   <li>参数校验异常 —— 汇总字段错误，返回 400 + {@link ResultCode#BAD_REQUEST}</li>
 *   <li>兜底 {@link Exception} —— 记录日志，返回 500，不泄露内部细节</li>
 * </ul>
 * 仅在 web 服务（classpath 有 servlet）装配。
 * 由 {@link WebAutoConfiguration} 注册为 Bean，避免下游服务无法扫描 common 包。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按错误码返回，消息已是 i18n 文案 */
    @ExceptionHandler(BaseException.class)
    public R<Void> handleBaseException(BaseException ex, HttpServletRequest req) {
        log.warn("[业务异常] {} - {} : {}", req.getRequestURI(), ex.getCode(), ex.getMessage());
        return R.error(ex.getCode(), ex.getMessage());
    }

    /** @Valid 校验失败（@RequestBody）：汇总字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.error(ResultCode.BAD_REQUEST.getCode(), detail);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBind(BindException ex) {
        String detail = ex.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.error(ResultCode.BAD_REQUEST.getCode(), detail);
    }

    /** 方法参数 @Validated 校验失败（@RequestParam/@PathVariable） */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraint(ConstraintViolationException ex) {
        return R.error(ResultCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return R.error(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数: " + ex.getParameterName());
    }

    /** 兜底：未预期异常，记录日志返回 500，不向外泄露堆栈 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("[未处理异常] {} : ", req.getRequestURI(), ex);
        return R.error(ResultCode.INTERNAL_ERROR);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
