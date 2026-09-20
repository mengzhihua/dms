package com.dms.common;

import static org.junit.jupiter.api.Assertions.*;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** 重启重复执行 data.sql 时种子数据不得翻倍：唯一约束缺失的表（客户/调研题）加约束后可安全重放。 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dms_seed_idem_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class SeedIdempotencyTest {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void replayingDataSqlDoesNotDuplicateRows() throws Exception {
        long customers = count("dms_customer");
        long questions = count("dms_survey_question");
        long parts = count("dms_part");
        assertTrue(customers > 0 && questions > 0 && parts > 0);

        ScriptUtils.executeSqlScript(
                dataSource.getConnection(),
                new EncodedResource(new ClassPathResource("data.sql")),
                true, true, "--", ";", "/*", "*/");

        assertEquals(customers, count("dms_customer"), "dms_customer 重复播种");
        assertEquals(questions, count("dms_survey_question"), "dms_survey_question 重复播种");
        assertEquals(parts, count("dms_part"), "dms_part 重复播种");
    }

    private long count(String table) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }
}
