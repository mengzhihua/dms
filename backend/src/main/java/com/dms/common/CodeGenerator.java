package com.dms.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 seq_no 表的并发安全单号生成器： 每条单据类型每天一行，UPDATE ... value = value + 1 原子自增。
 */
@Component
public class CodeGenerator {
    private final JdbcTemplate jdbc;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    public CodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 生成形如 WO20240115-0007 的单号，key 维度为 prefix+日期，保证日序列唯一。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String prefix) {
        String day = LocalDate.now().format(DAY);
        String key = prefix + day;
        int updated = jdbc.update("UPDATE seq_no SET seq_value = seq_value + 1 WHERE name = ?", key);
        if (updated == 0) {
            try {
                jdbc.update("INSERT INTO seq_no(name, seq_value) VALUES(?, 1)", key);
            } catch (DuplicateKeyException e) {
                jdbc.update("UPDATE seq_no SET seq_value = seq_value + 1 WHERE name = ?", key);
            }
        }
        Long v = jdbc.queryForObject("SELECT seq_value FROM seq_no WHERE name = ?", Long.class, key);
        long n = v == null ? 1 : v;
        return String.format("%s%s-%04d", prefix, day, n);
    }
}
