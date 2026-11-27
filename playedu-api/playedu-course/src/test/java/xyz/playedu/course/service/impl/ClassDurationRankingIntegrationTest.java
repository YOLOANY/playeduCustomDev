/*
 * Copyright (C) 2023 杭州白书科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.playedu.course.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import xyz.playedu.common.domain.Department;
import xyz.playedu.common.service.DepartmentService;
import xyz.playedu.common.service.UserService;
import xyz.playedu.course.domain.ClassDurationRank;
import xyz.playedu.course.mapper.UserLearnDurationStatsMapper;

@SpringBootTest(
        classes = ClassDurationRankingIntegrationTest.TestApplication.class,
        properties = {
            "spring.main.web-application-type=none",
            "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/playedu_credit_learning_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
            "spring.datasource.username=${CREDIT_TEST_DB_USERNAME:root}",
            "spring.datasource.password=${CREDIT_TEST_DB_PASSWORD}",
            "spring.datasource.hikari.maximum-pool-size=10",
            "mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml",
            "mybatis-plus.global-config.banner=false"
        })
@Sql(
        scripts = "classpath:class-ranking-test-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ClassDurationRankingIntegrationTest {
    private static final int COLLEGE = 9200;
    private static final int CLASS_A = 9201;
    private static final int CLASS_B = 9202;
    private static final int CLASS_C = 9203;
    private static final int OTHER_COLLEGE = 9208;
    private static final int OTHER_CLASS = 9209;
    private static final int USER_A = 9201;
    private static final int USER_B = 9202;

    @Autowired private UserLearnDurationStatsServiceImpl statsService;
    @Autowired private UserLearnDurationStatsMapper statsMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SqlSessionFactory sqlSessionFactory;

    @MockBean private UserService userService;
    @MockBean private DepartmentService departmentService;

    @BeforeEach
    void resetSyntheticData() {
        jdbcTemplate.update("DELETE FROM user_learn_duration_stats WHERE user_id BETWEEN 9201 AND 9210");
        jdbcTemplate.update("DELETE FROM user_department WHERE dep_id BETWEEN 9200 AND 9209");
        jdbcTemplate.update("DELETE FROM users WHERE id BETWEEN 9201 AND 9210");
        jdbcTemplate.update("DELETE FROM departments WHERE id BETWEEN 9200 AND 9209");
        insertDepartment(COLLEGE, "合成学院", 0);
        insertDepartment(CLASS_A, "合成一班", COLLEGE);
        insertDepartment(CLASS_B, "合成二班", COLLEGE);
        insertDepartment(CLASS_C, "合成空班", COLLEGE);
        insertDepartment(OTHER_COLLEGE, "其他学院", 0);
        insertDepartment(OTHER_CLASS, "其他班级", OTHER_COLLEGE);
    }

    @Test
    void calculatesAverageAcrossAllActiveMembersIncludingZeroDuration() {
        insertUser(USER_A, 1, 0, 0);
        insertUser(USER_B, 1, 0, 0);
        insertUser(9203, 0, 0, 0);
        insertUser(9204, 1, 0, 1);
        insertUser(9205, 1, 0, 0);
        insertUser(9206, 1, 1, 0);
        joinClass(USER_A, CLASS_A);
        joinClass(USER_A, CLASS_A);
        joinClass(USER_B, CLASS_A);
        joinClass(9203, CLASS_A);
        joinClass(9204, CLASS_A);
        joinClass(9205, CLASS_B);
        joinClass(9206, CLASS_B);
        LocalDate today = LocalDate.now();
        insertDuration(USER_A, today, 600000L);
        insertDuration(9203, today, 9000000L);
        insertDuration(9204, today, 9000000L);
        insertDuration(9205, today, 1200000L);
        insertDuration(9206, today, 9000000L);

        List<ClassDurationRank> result =
                syntheticRows(
                        statsMapper.classDurationRanking(
                                COLLEGE, today, today.plusDays(1)));

        assertEquals(List.of(CLASS_B, CLASS_A, CLASS_C), depIds(result));
        ClassDurationRank classA = byId(result).get(CLASS_A);
        assertEquals(2L, classA.getStudentCount());
        assertEquals(600000L, classA.getTotalDuration());
        assertEquals(300000L, classA.getAverageDuration());
        ClassDurationRank classB = byId(result).get(CLASS_B);
        assertEquals(1L, classB.getStudentCount());
        assertEquals(1200000L, classB.getAverageDuration());
        ClassDurationRank emptyClass = byId(result).get(CLASS_C);
        assertEquals(0L, emptyClass.getStudentCount());
        assertEquals(0L, emptyClass.getTotalDuration());
        assertEquals(0L, emptyClass.getAverageDuration());
    }

    @Test
    void appliesAllFourDateRangesWithExclusiveEndDates() {
        insertUser(USER_A, 1, 0, 0);
        joinClass(USER_A, CLASS_A);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<LocalDate, Long> durations = new LinkedHashMap<>();
        durations.put(today, 1000L);
        durations.put(today.minusDays(1), 2000L);
        durations.put(weekStart, 4000L);
        durations.put(monthStart, 8000L);
        durations.put(today.plusDays(1), 16000L);
        durations.put(monthStart.plusMonths(1), 32000L);
        durations.put(monthStart.minusDays(1), 64000L);
        durations.put(LocalDate.of(2020, 1, 1), 128000L);
        durations.forEach((date, duration) -> insertDuration(USER_A, date, duration));

        assertRange("today", today, today.plusDays(1), durations);
        assertRange("week", weekStart, weekStart.plusWeeks(1), durations);
        assertRange("month", monthStart, monthStart.plusMonths(1), durations);
        assertRange("all", null, null, durations);
    }

    @Test
    void limitsRankingToSiblingClassesAndAssignsTiedRanks() throws Exception {
        insertUser(USER_A, 1, 0, 0);
        insertUser(USER_B, 1, 0, 0);
        insertUser(9205, 1, 0, 0);
        joinClass(USER_A, CLASS_A);
        joinClass(USER_B, CLASS_B);
        joinClass(9205, OTHER_CLASS);
        insertDuration(USER_A, LocalDate.of(2026, 1, 1), 1000L);
        insertDuration(USER_B, LocalDate.of(2026, 1, 2), 1000L);
        insertDuration(9205, LocalDate.of(2026, 1, 3), 999999L);
        allowCurrentClass();

        List<ClassDurationRank> result = statsService.classDurationRanking(USER_A, CLASS_A, "all");

        assertEquals(List.of(CLASS_A, CLASS_B, CLASS_C), depIds(result));
        assertFalse(depIds(result).contains(OTHER_CLASS));
        assertEquals(1, result.get(0).getRank());
        assertEquals(1, result.get(1).getRank());
        assertEquals(3, result.get(2).getRank());
        assertTrue(result.get(0).getCurrent());
        assertFalse(result.get(1).getCurrent());
    }

    @Test
    void rankingQueryCanUseAvailableDurationIndex() {
        Map<String, Object> params = new HashMap<>();
        params.put("parentId", COLLEGE);
        params.put("startDate", LocalDate.now());
        params.put("endDate", LocalDate.now().plusDays(1));
        MappedStatement statement =
                sqlSessionFactory
                        .getConfiguration()
                        .getMappedStatement(
                                "xyz.playedu.course.mapper.UserLearnDurationStatsMapper.classDurationRanking");
        BoundSql boundSql = statement.getBoundSql(params);
        List<Object> arguments = new ArrayList<>();
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String property = mapping.getProperty();
            arguments.add(
                    boundSql.hasAdditionalParameter(property)
                            ? boundSql.getAdditionalParameter(property)
                            : params.get(property));
        }

        List<Map<String, Object>> plan =
                jdbcTemplate.queryForList("EXPLAIN " + boundSql.getSql(), arguments.toArray());

        Map<String, Object> durationRow =
                plan.stream()
                        .filter(item -> "s".equals(item.get("table")))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("EXPLAIN 缺少学习时长表"));
        Number estimatedRows = (Number) durationRow.get("rows");
        assertTrue(
                durationRow.get("key") != null
                        || (estimatedRows != null && estimatedRows.longValue() <= 10),
                () -> "学习时长表未使用索引且不是小表：" + durationRow);
    }

    private void assertRange(
            String range,
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, Long> durations) {
        List<ClassDurationRank> result =
                syntheticRows(statsMapper.classDurationRanking(COLLEGE, startDate, endDate));
        long expected =
                durations.entrySet().stream()
                        .filter(
                                entry ->
                                        (startDate == null || !entry.getKey().isBefore(startDate))
                                                && (endDate == null
                                                        || entry.getKey().isBefore(endDate)))
                        .mapToLong(Map.Entry::getValue)
                        .sum();
        ClassDurationRank classA = byId(result).get(CLASS_A);
        assertEquals(expected, classA.getTotalDuration(), range);
        assertEquals(expected, classA.getAverageDuration(), range);
        assertEquals(1L, classA.getStudentCount(), range);
    }

    private void allowCurrentClass() throws Exception {
        when(userService.getDepIdsByUserId(USER_A)).thenReturn(List.of(CLASS_A));
        Department department = new Department();
        department.setId(CLASS_A);
        department.setParentId(COLLEGE);
        when(departmentService.findOrFail(CLASS_A)).thenReturn(department);
    }

    private List<ClassDurationRank> syntheticRows(List<ClassDurationRank> rows) {
        return rows.stream()
                .filter(item -> item.getDepId() >= CLASS_A && item.getDepId() <= CLASS_C)
                .toList();
    }

    private List<Integer> depIds(List<ClassDurationRank> rows) {
        return rows.stream().map(ClassDurationRank::getDepId).toList();
    }

    private Map<Integer, ClassDurationRank> byId(List<ClassDurationRank> rows) {
        return rows.stream()
                .collect(Collectors.toMap(ClassDurationRank::getDepId, Function.identity()));
    }

    private void insertDepartment(int id, String name, int parentId) {
        jdbcTemplate.update(
                "INSERT INTO departments (id, name, parent_id, parent_chain, sort) VALUES (?, ?, ?, '', 0)",
                id,
                name,
                parentId);
    }

    private void insertUser(int id, int active, int locked, int deleted) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, name, is_active, is_lock, deleted) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                "class.rank." + id + "@example.com",
                "合成学员" + id,
                active,
                locked,
                deleted);
    }

    private void joinClass(int userId, int depId) {
        jdbcTemplate.update(
                "INSERT INTO user_department (user_id, dep_id) VALUES (?, ?)", userId, depId);
    }

    private void insertDuration(int userId, LocalDate date, long duration) {
        jdbcTemplate.update(
                "INSERT INTO user_learn_duration_stats (user_id, duration, created_date) VALUES (?, ?, ?)",
                userId,
                duration,
                Date.valueOf(date));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {"xyz.playedu.course.mapper"})
    @Import(UserLearnDurationStatsServiceImpl.class)
    static class TestApplication {}
}
