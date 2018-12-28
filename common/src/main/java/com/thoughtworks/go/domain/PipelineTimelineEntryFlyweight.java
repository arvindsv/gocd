/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import java.util.List;
import java.util.Map;

public class PipelineTimelineEntryFlyweight implements PipelineTimelineEntry {
    private PipelineTimelineEntry pipelineTimelineEntry = null;
    private PipelineTimelineEntryLoader loader;
    private long idAndOtherThingsNeededToLoad;

    public PipelineTimelineEntryFlyweight(PipelineTimelineEntryLoader loader, long idAndOtherThingsNeededToLoad) {
        this.loader = loader;
        this.idAndOtherThingsNeededToLoad = idAndOtherThingsNeededToLoad;
    }

    public int compareTo(Object o) {
        return getEntry().compareTo(o);
    }

    public int getCounter() {
        return getEntry().getCounter();
    }

    public PipelineTimelineEntry insertedBefore() {
        return getEntry().insertedBefore();
    }

    public PipelineTimelineEntry insertedAfter() {
        return getEntry().insertedAfter();
    }

    public void setInsertedBefore(PipelineTimelineEntry insertedBefore) {
        getEntry().setInsertedBefore(insertedBefore);
    }

    public void setInsertedAfter(PipelineTimelineEntry insertedAfter) {
        getEntry().setInsertedAfter(insertedAfter);
    }

    public String getPipelineName() {
        return getEntry().getPipelineName();
    }

    public Long getId() {
        return getEntry().getId();
    }

    public PipelineTimelineEntry previous() {
        return getEntry().previous();
    }

    public double naturalOrder() {
        return getEntry().naturalOrder();
    }

    public void updateNaturalOrder() {
        getEntry().updateNaturalOrder();
    }

    public boolean hasBeenUpdated() {
        return getEntry().hasBeenUpdated();
    }

    public PipelineIdentifier getPipelineLocator() {
        return getEntry().getPipelineLocator();
    }

    public Map<String, List<PipelineTimelineEntry.Revision>> revisions() {
        return getEntry().revisions();
    }

    public double determinedNaturalOrder() {
        return getEntry().determinedNaturalOrder();
    }

    private PipelineTimelineEntry getEntry() {
        if (pipelineTimelineEntry == null) {
            pipelineTimelineEntry = loader.load(idAndOtherThingsNeededToLoad);
        }
        return pipelineTimelineEntry;
    }
}
