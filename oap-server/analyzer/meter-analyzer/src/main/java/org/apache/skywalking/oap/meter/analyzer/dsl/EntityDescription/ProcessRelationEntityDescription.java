/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.meter.analyzer.dsl.EntityDescription;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.skywalking.oap.server.core.analysis.meter.ScopeType;

import java.util.List;

@Getter
@RequiredArgsConstructor
@ToString
public class ProcessRelationEntityDescription implements EntityDescription {
    private final ScopeType scopeType = ScopeType.PROCESS_RELATION;
    private final List<String> serviceKeys;
    private final List<String> instanceKeys;
    private final String sourceProcessIdKey;
    private final String destProcessIdKey;
    private final String detectPointKey;
    private final String delimiter;

    @Override
    public List<String> getLabelKeys() {
        return ImmutableList.<String>builder()
                .addAll(serviceKeys)
                .addAll(instanceKeys)
                .add(detectPointKey, sourceProcessIdKey, destProcessIdKey).build();
    }
}
