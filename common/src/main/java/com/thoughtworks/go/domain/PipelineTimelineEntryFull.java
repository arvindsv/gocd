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

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands a pipeline which can be compared based on its material checkin (natural) order
 */
public class PipelineTimelineEntryFull implements PipelineTimelineEntry, Comparable {
    private final String pipelineName;
    private final long id;
    private final int counter;
    private final Map<String, List<Revision>> revisions;
    private PipelineTimelineEntry insertedBefore;
    private PipelineTimelineEntry insertedAfter;
    private double naturalOrder = 0.0;
    private boolean hasBeenUpdated;

    public PipelineTimelineEntryFull(String pipelineName, long id, int counter, Map<String, List<Revision>> revisions) {
        this.pipelineName = pipelineName;
        this.id = id;
        this.counter = counter;
        this.revisions = revisions;
    }

    public PipelineTimelineEntryFull(String pipelineName, long id, Integer counter, Map<String, List<Revision>> revisions, double naturalOrder) {
        this(pipelineName, id, counter, revisions);
        this.naturalOrder = naturalOrder;
    }

    public int compareTo(Object o) {
        if (o == null) {
            throw new NullPointerException("Cannot compare this object with null");
        }
        if (o.getClass() != this.getClass()) {
            throw new RuntimeException("Cannot compare '" + o + "' with '" + this + "'");
        }
        if (this.equals(o)) {
            return 0;
        }

        PipelineTimelineEntryFull that = (PipelineTimelineEntryFull) o;
        Map<Date, TreeSet<Integer>> earlierMods = new HashMap<>();

        for (String materialFlyweight : revisions.keySet()) {
            List<Revision> thisRevs = this.revisions.get(materialFlyweight);
            List<Revision> thatRevs = that.revisions.get(materialFlyweight);
            if (thisRevs == null || thatRevs == null) {
                continue;
            }
            Revision thisRevision = thisRevs.get(0);
            Revision thatRevision = thatRevs.get(0);
            if (thisRevision == null || thatRevision == null) {
                continue;
            }
            Date thisDate = thisRevision.date;
            Date thatDate = thatRevision.date;
            if (thisDate.equals(thatDate)) {
                continue;
            }
            populateEarlierModification(earlierMods, thisDate, thatDate);
        }
        if (earlierMods.isEmpty()) {
            return counter < that.counter ? -1 : 1;
        }
        TreeSet<Date> sortedModDate = new TreeSet<>(earlierMods.keySet());
        if (hasContentionOnEarliestMod(earlierMods, sortedModDate.first())) {
            return counter < that.counter ? -1 : 1;
        }
        return earlierMods.get(sortedModDate.first()).first();
    }

    public int getCounter() {
        return counter;
    }

    private void populateEarlierModification(Map<Date, TreeSet<Integer>> earlierMods, Date thisDate, Date thatDate) {
        int value = thisDate.before(thatDate) ? -1 : 1;
        Date actual = thisDate.before(thatDate) ? thisDate : thatDate;
        if (!earlierMods.containsKey(actual)) {
            earlierMods.put(actual, new TreeSet<>());
        }
        earlierMods.get(actual).add(value);
    }

    private boolean hasContentionOnEarliestMod(Map<Date, TreeSet<Integer>> earlierMods, Date earliestModDate) {
        return earlierMods.get(earliestModDate).size() > 1;
    }

    public PipelineTimelineEntry insertedBefore() {
        return insertedBefore;
    }

    public PipelineTimelineEntry insertedAfter() {
        return insertedAfter;
    }

    public void setInsertedBefore(PipelineTimelineEntry insertedBefore) {
        if (this.insertedBefore != null) {
            throw bomb("cannot change insertedBefore for: " + this + " with " + insertedBefore);
        }
        this.insertedBefore = insertedBefore;
    }

    public void setInsertedAfter(PipelineTimelineEntry insertedAfter) {
        if (this.insertedAfter != null) {
            throw bomb("cannot change insertedAfter for: " + this + " with " + insertedAfter);
        }
        this.insertedAfter = insertedAfter;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineTimelineEntryFull that = (PipelineTimelineEntryFull) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override public String toString() {
        return "PipelineTimelineEntry{" +
                "pipelineName='" + pipelineName + '\'' +
                ", id=" + id +
                ", counter=" + counter +
                ", revisions=" + revisions +
                ", naturalOrder=" + naturalOrder +
                '}';
    }

    public PipelineTimelineEntry previous() {
        return insertedAfter();
    }

    public double naturalOrder() {
        return naturalOrder;
    }

    public void updateNaturalOrder() {
        double calculatedOrder = calculateNaturalOrder();
        if (this.naturalOrder > 0.0 && this.naturalOrder != calculatedOrder) {
            bomb(String.format("Calculated natural ordering %s is not the same as the existing naturalOrder %s, for pipeline %s, with id %s", calculatedOrder, this.naturalOrder, this.pipelineName, this.id));
        }
        if (this.naturalOrder == 0.0 && this.naturalOrder != calculatedOrder) {
            this.naturalOrder = calculatedOrder;
            this.hasBeenUpdated = true;
        }
    }

    public boolean hasBeenUpdated() {
        return this.hasBeenUpdated;
    }

    private double calculateNaturalOrder() {
        double previous = 0.0;
        if (insertedAfter != null) {
            previous = insertedAfter.determinedNaturalOrder();
        }
        if (insertedBefore != null) {
            return (previous + insertedBefore.determinedNaturalOrder()) / 2.0;
        } else {
            return previous + 1.0;
        }
    }

    public PipelineIdentifier getPipelineLocator() {
        return new PipelineIdentifier(pipelineName, counter, null);
    }

    public Map<String, List<Revision>> revisions() {
        return revisions;
    }

    @Override
    public double determinedNaturalOrder() {
        return naturalOrder;
    }
}
