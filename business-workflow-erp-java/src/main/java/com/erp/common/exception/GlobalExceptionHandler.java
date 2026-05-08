package com.erp.common.exception;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.result.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理Spring Security认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常：{}", e.getMessage());
        if (e instanceof BadCredentialsException) {
            return Result.error(ResultCodeEnum.LOGIN_ERROR.getCode(), "用户名或密码错误");
        } else if (e instanceof DisabledException) {
            return Result.error(ResultCodeEnum.LOGIN_ERROR.getCode(), "账号已被禁用");
        } else if (e instanceof LockedException) {
            return Result.error(ResultCodeEnum.LOGIN_ERROR.getCode(), "账号已被锁定");
        }
        return Result.error(ResultCodeEnum.UNAUTHORIZED.getCode(), "认证失败");
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常：{}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理参数校验异常（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        log.error("参数绑定异常：{}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理参数校验异常（@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("参数约束异常：{}", e.getMessage());
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.iterator().hasNext() 
            ? violations.iterator().next().getMessage() 
            : "参数校验失败";
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理JWT Token过期异常
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public Result<?> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("JWT Token已过期：{}", e.getMessage());
        return Result.error(ResultCodeEnum.TOKEN_EXPIRED.getCode(), ResultCodeEnum.TOKEN_EXPIRED.getMessage());
    }

    /**
     * 处理JWT Token格式错误异常
     */
    @ExceptionHandler(MalformedJwtException.class)
    public Result<?> handleMalformedJwtException(MalformedJwtException e) {
        log.warn("JWT Token格式错误：{}", e.getMessage());
        return Result.error(ResultCodeEnum.TOKEN_INVALID.getCode(), ResultCodeEnum.TOKEN_INVALID.getMessage());
    }

    /**
     * 处理不支持的JWT Token异常
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public Result<?> handleUnsupportedJwtException(UnsupportedJwtException e) {
        log.warn("不支持的JWT Token：{}", e.getMessage());
        return Result.error(ResultCodeEnum.TOKEN_INVALID.getCode(), ResultCodeEnum.TOKEN_INVALID.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error(ResultCodeEnum.INTERNAL_SERVER_ERROR.getCode(), "系统异常，请联系管理员");
    }
}





















