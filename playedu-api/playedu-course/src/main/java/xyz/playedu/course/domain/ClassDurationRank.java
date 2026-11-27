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
package xyz.playedu.course.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClassDurationRank {
    @JsonProperty("dep_id")
    private Integer depId;

    @JsonProperty("dep_name")
    private String depName;

    @JsonProperty("student_count")
    private Long studentCount;

    @JsonProperty("total_duration")
    private Long totalDuration;

    @JsonProperty("average_duration")
    private Long averageDuration;

    private Integer rank;

    @JsonProperty("is_current")
    private Boolean current;
}
