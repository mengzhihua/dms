package com.dms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<Void> biz(BizException e) {
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> dup(DuplicateKeyException e) {
        return R.fail(400, "编码已存在，请勿重复");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> valid(BindException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + " " + f.getDefaultMessage())
                        .findFirst()
                        .orElse("参数校验失败");
        return R.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> other(Exception e) {
        log.error("Unhandled error", e);
        return R.fail(500, e.getMessage());
    }
}
