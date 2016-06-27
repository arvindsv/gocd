/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/* Understands how to cache dashboard statuses, for every pipeline. */
@Component
public class GoDashboardCache {
    /**
     * Assumption: The put(), putAll() and replaceAllEntriesInCacheWith() methods, which change this cache,
     * will always be called from the same thread (queueProcessor in GoDashboardActivityListener). So, not surrounding
     * it with a synchronizedMap. Also, uses {@link LinkedHashMap} to preserve insertion order.
     */
    private LinkedHashMap<CaseInsensitiveString, GoDashboardPipelineModel> cache;
    private volatile List<GoDashboardPipelineModel> orderedEntries;
}
