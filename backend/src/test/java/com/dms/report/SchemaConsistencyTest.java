package com.dms.report;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 保证 H2 版 schema.sql（CREATE + ALTER ADD COLUMN）与 MySQL 版 schema-mysql.sql
 * 的「表名, 列名」集合完全一致，防止两份 DDL 漂移。
 */
class SchemaConsistencyTest {

    private static final Pattern CREATE = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS (\\w+)\\s*\\((.*?)\\)\\s*(?:ENGINE|;)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_ADD = Pattern.compile(
            "ALTER TABLE (\\w+) ADD COLUMN IF NOT EXISTS (\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> NON_COL =
            new LinkedHashSet<>(
                    java.util.Arrays.asList(
                            "PRIMARY", "UNIQUE", "KEY", "CONSTRAINT", "INDEX", "FOREIGN", "CHECK"));

    /** 解析建表文件，返回 (table, column) 集合；约束行忽略。 */
    private Set<String> columns(String file) throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        Matcher m = CREATE.matcher(sql);
        while (m.find()) {
            String table = m.group(1).toLowerCase();
            for (String raw : m.group(2).split("\n")) {
                String line = raw.replaceAll("--.*$", "").trim().replace("`", "");
                if (line.isEmpty()) continue;
                String first = line.split("\\s+")[0].toLowerCase();
                if (NON_COL.contains(first.toUpperCase())) continue;
                out.add(table + "." + first.replaceAll("[,;]$", ""));
            }
        }
        Matcher a = ALTER_ADD.matcher(sql);
        while (a.find()) {
            out.add(a.group(1).toLowerCase() + "." + a.group(2).toLowerCase());
        }
        return out;
    }

    @Test
    void h2AndMysqlSchemasHaveSameColumns() throws Exception {
        assertEquals(
                columns("src/main/resources/schema.sql"),
                columns("src/main/resources/schema-mysql.sql"),
                "schema.sql 与 schema-mysql.sql 列集合不一致");
    }
}
