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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import xyz.playedu.common.types.paginate.PaginationResult;
import xyz.playedu.course.domain.UserLearningTrack;

@SpringBootTest(
        classes = UserLearningTrackIntegrationTest.TestApplication.class,
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
        scripts = "classpath:learning-tracks-test-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserLearningTrackIntegrationTest {
    private static final int USER_ID = 9301;
    private static final int OTHER_USER_ID = 9302;

    @Autowired private UserCourseHourRecordServiceImpl recordService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSyntheticData() {
        jdbcTemplate.update(
                "DELETE FROM user_course_hour_records WHERE id BETWEEN 9300 AND 9399");
        jdbcTemplate.update("DELETE FROM course_hour WHERE id BETWEEN 9300 AND 9399");
        jdbcTemplate.update("DELETE FROM courses WHERE id BETWEEN 9300 AND 9399");
    }

    @Test
    void excludesOrphanRecordsFromPagesAndTotal() {
        insertCourse(9301, "正常课程一");
        insertCourse(9302, "正常课程二");
        insertCourse(9303, "待删除课程");
        insertHour(9301, 9301, "较早完成课时", "video");
        insertHour(9302, 9302, "较晚完成课时", "document");
        insertHour(9303, 9301, "待删除课时", "video");
        insertHour(9304, 9303, "缺课程课时", "video");
        insertHour(9305, 9301, "未完成课时", "video");
        insertHour(9306, 9301, "无完成时间课时", "video");
        insertHour(9307, 9301, "其他用户课时", "video");

        insertRecord(9301, USER_ID, 9301, 9301, 1, "2026-01-01 10:00:00");
        insertRecord(9302, USER_ID, 9302, 9302, 1, "2026-02-01 10:00:00");
        insertRecord(9303, USER_ID, 9301, 9303, 1, "2026-03-01 10:00:00");
        insertRecord(9304, USER_ID, 9303, 9304, 1, "2026-04-01 10:00:00");
        insertRecord(9305, USER_ID, 9301, 9305, 0, "2026-05-01 10:00:00");
        insertRecord(9306, USER_ID, 9301, 9306, 1, null);
        insertRecord(9307, OTHER_USER_ID, 9301, 9307, 1, "2026-06-01 10:00:00");

        jdbcTemplate.update("DELETE FROM course_hour WHERE id = 9303");
        jdbcTemplate.update("DELETE FROM courses WHERE id = 9303");

        PaginationResult<UserLearningTrack> result = recordService.learningTracks(USER_ID, 1, 20);

        assertEquals(2L, result.getTotal());
        assertEquals(
                List.of(9302, 9301),
                result.getData().stream().map(UserLearningTrack::getRecordId).toList());
        UserLearningTrack latest = result.getData().get(0);
        assertEquals(9302, latest.getCourseId());
        assertEquals("正常课程二", latest.getCourseTitle());
        assertEquals(9302, latest.getHourId());
        assertEquals("较晚完成课时", latest.getHourTitle());
        assertEquals("document", latest.getHourType());
        assertEquals(
                Timestamp.valueOf("2026-02-01 10:00:00").getTime(),
                latest.getFinishedAt().getTime());

        PaginationResult<UserLearningTrack> firstPage = recordService.learningTracks(USER_ID, 1, 1);
        PaginationResult<UserLearningTrack> secondPage = recordService.learningTracks(USER_ID, 2, 1);
        PaginationResult<UserLearningTrack> thirdPage = recordService.learningTracks(USER_ID, 3, 1);
        assertEquals(2L, firstPage.getTotal());
        assertEquals(2L, secondPage.getTotal());
        assertEquals(2L, thirdPage.getTotal());
        assertEquals(List.of(9302), recordIds(firstPage));
        assertEquals(List.of(9301), recordIds(secondPage));
        assertTrue(thirdPage.getData().isEmpty());
        assertEquals(2L, orphanRecordCount());
    }

    private List<Integer> recordIds(PaginationResult<UserLearningTrack> result) {
        return result.getData().stream().map(UserLearningTrack::getRecordId).toList();
    }

    private Long orphanRecordCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_course_hour_records WHERE id IN (9303, 9304)",
                Long.class);
    }

    private void insertCourse(Integer id, String title) {
        jdbcTemplate.update("INSERT INTO courses (id, title) VALUES (?, ?)", id, title);
    }

    private void insertHour(Integer id, Integer courseId, String title, String type) {
        jdbcTemplate.update(
                "INSERT INTO course_hour (id, course_id, title, type) VALUES (?, ?, ?, ?)",
                id,
                courseId,
                title,
                type);
    }

    private void insertRecord(
            Integer id,
            Integer userId,
            Integer courseId,
            Integer hourId,
            Integer isFinished,
            String finishedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO user_course_hour_records
                    (id, user_id, course_id, hour_id, total_duration, finished_duration,
                     real_duration, is_finished, finished_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 600, 600, 600, ?, ?, NOW(), NOW())
                """,
                id,
                userId,
                courseId,
                hourId,
                isFinished,
                finishedAt == null ? null : Timestamp.valueOf(finishedAt));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {"xyz.playedu.course.mapper"})
    @Import(UserCourseHourRecordServiceImpl.class)
    static class TestApplication {}
}
