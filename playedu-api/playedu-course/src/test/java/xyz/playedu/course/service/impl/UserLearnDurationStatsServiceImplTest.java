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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.playedu.common.domain.Department;
import xyz.playedu.common.exception.NotFoundException;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.service.DepartmentService;
import xyz.playedu.common.service.UserService;
import xyz.playedu.course.domain.ClassDurationRank;
import xyz.playedu.course.mapper.UserLearnDurationStatsMapper;

@ExtendWith(MockitoExtension.class)
class UserLearnDurationStatsServiceImplTest {
    @Mock private UserService userService;
    @Mock private DepartmentService departmentService;
    @Mock private UserLearnDurationStatsMapper statsMapper;

    @Spy @InjectMocks private UserLearnDurationStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", statsMapper);
    }

    @Test
    void rejectsClassesOutsideCurrentStudentsMembership() {
        assertThrows(
                ServiceException.class, () -> service.classDurationRanking(7, 11, "today"));
        verifyNoInteractions(statsMapper);
    }

    @Test
    void usesHalfOpenTodayRangeAndHighlightsSelectedClass() throws NotFoundException {
        allowSelectedClass();
        LocalDate today = LocalDate.of(2026, 11, 20);
        doReturn(today).when(service).currentDate();
        ClassDurationRank item = rank(10, 1200L);
        when(statsMapper.classDurationRanking(99, today, today.plusDays(1)))
                .thenReturn(List.of(item));

        List<ClassDurationRank> result = service.classDurationRanking(7, 10, "today");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getRank());
        assertTrue(result.get(0).getCurrent());
    }

    @Test
    void resolvesWeekMonthAndAllRanges() throws NotFoundException {
        allowSelectedClass();
        LocalDate today = LocalDate.of(2026, 11, 22);
        doReturn(today).when(service).currentDate();
        when(statsMapper.classDurationRanking(any(), any(), any())).thenReturn(List.of());

        service.classDurationRanking(7, 10, "week");
        verify(statsMapper)
                .classDurationRanking(
                        99, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 23));

        service.classDurationRanking(7, 10, "month");
        verify(statsMapper)
                .classDurationRanking(
                        99, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));

        service.classDurationRanking(7, 10, "all");
        verify(statsMapper).classDurationRanking(99, null, null);
    }

    @Test
    void rejectsUnknownRange() throws NotFoundException {
        allowSelectedClass();
        assertThrows(ServiceException.class, () -> service.classDurationRanking(7, 10, "year"));
        verifyNoInteractions(statsMapper);
    }

    @Test
    void assignsCompetitionRanksAndOnlyHighlightsSelectedClass() throws NotFoundException {
        allowSelectedClass();
        doReturn(LocalDate.of(2026, 11, 20)).when(service).currentDate();
        ClassDurationRank first = rank(11, 900L);
        ClassDurationRank second = rank(10, 900L);
        ClassDurationRank third = rank(12, null);
        when(statsMapper.classDurationRanking(any(), any(), any()))
                .thenReturn(List.of(first, second, third));

        List<ClassDurationRank> result = service.classDurationRanking(7, 10, "all");

        assertEquals(1, result.get(0).getRank());
        assertEquals(1, result.get(1).getRank());
        assertEquals(3, result.get(2).getRank());
        assertFalse(result.get(0).getCurrent());
        assertTrue(result.get(1).getCurrent());
        assertFalse(result.get(2).getCurrent());
        assertEquals(0L, result.get(2).getAverageDuration());
    }

    private void allowSelectedClass() throws NotFoundException {
        when(userService.getDepIdsByUserId(7)).thenReturn(List.of(10));
        Department department = new Department();
        department.setId(10);
        department.setParentId(99);
        when(departmentService.findOrFail(10)).thenReturn(department);
    }

    private ClassDurationRank rank(Integer depId, Long averageDuration) {
        ClassDurationRank item = new ClassDurationRank();
        item.setDepId(depId);
        item.setAverageDuration(averageDuration);
        return item;
    }
}
