package com.dms.common;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String INTEGRITY_MESSAGE = "数据不完整或重复，请检查必填项/唯一字段";

    @ExceptionHandler(BizException.class)
    public R<Void> biz(BizException e, javax.servlet.http.HttpServletResponse res) {
        if (e.getCode() == 401 || e.getCode() == 403 || e.getCode() == 404 || e.getCode() == 429) {
            res.setStatus(e.getCode());
        }
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public R<Void> integrity(Exception e) {
        return R.fail(400, INTEGRITY_MESSAGE);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> valid(BindException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + " " + f.getDefaultMessage())
                        .collect(Collectors.joining("；"));
        return R.fail(400, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public R<Void> badBody(Exception e, javax.servlet.http.HttpServletResponse res) {
        res.setStatus(400);
        return R.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> other(Exception e) {
        log.error("Unhandled error", e);
        return R.fail(500, "服务器处理失败");
    }
}
