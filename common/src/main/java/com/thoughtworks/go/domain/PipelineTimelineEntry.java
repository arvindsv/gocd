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

package com.thoughtworks.go.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands a pipeline which can be compared based on its material checkin (natural) order
 */
public interface PipelineTimelineEntry {
    int compareTo(Object o);

    int getCounter();

    PipelineTimelineEntry insertedBefore();

    PipelineTimelineEntry insertedAfter();

    void setInsertedBefore(PipelineTimelineEntry insertedBefore);

    void setInsertedAfter(PipelineTimelineEntry insertedAfter);

    String getPipelineName();

    Long getId();

    PipelineTimelineEntry previous();

    double naturalOrder();

    void updateNaturalOrder();

    boolean hasBeenUpdated();

    PipelineIdentifier getPipelineLocator();

    Map<String, List<Revision>> revisions();

    double determinedNaturalOrder();

    class Revision {
        public final Date date;
        public final String revision;
        public final String folder;
        public final long id;

        public Revision(Date date, String revision, String folder, long id) {
            this.date = date;
            this.revision = revision;
            this.folder = folder;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Revision revision1 = (Revision) o;

            if (date != null ? !date.equals(revision1.date) : revision1.date != null) {
                return false;
            }
            if (revision != null ? !revision.equals(revision1.revision) : revision1.revision != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = date != null ? date.hashCode() : 0;
            result = 31 * result + (revision != null ? revision.hashCode() : 0);
            result = 31 * result + (folder != null ? folder.hashCode() : 0);
            return result;
        }

        @Override public String toString() {
            return "Revision{" +
                    "date=" + date +
                    ", revision='" + revision + '\'' +
                    ", folder='" + folder + '\'' +
                    '}';
        }

        public boolean lessThan(Revision revision) {
            if (this == revision) {
                return true;
            }

//            if (!folder.equals(revision.folder)) {
//                return false;
//            }

            if (date.compareTo(revision.date) < 0) {
                return true;
            }

            return false;
        }
    }
}
