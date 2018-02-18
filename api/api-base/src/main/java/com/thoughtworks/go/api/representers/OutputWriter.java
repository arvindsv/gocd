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

package com.thoughtworks.go.api.representers;

import java.io.Closeable;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

public interface OutputWriter extends Closeable {
    OutputWriter add(String key, String value);

    OutputWriter addIfNotNull(String key, String value);

    OutputWriter add(String key, int value);

    OutputWriter add(String key, boolean value);

    OutputWriter add(String key, long value);

    OutputWriter add(String key, Date value);

    OutputWriter addIfNotNull(String key, Date value);

    OutputWriter addChild(String key, Consumer<OutputWriter> consumer);

    OutputWriter addChildList(String key, Consumer<OutputWriter.OutputListWriter> listWriterConsumer);

    OutputWriter addChildList(String key, Collection<String> values);

    OutputWriter addLinks(Consumer<OutputWriter.OutputLinkWriter> consumer);

    interface OutputListWriter {
        OutputWriter.OutputListWriter value(String value);

        OutputWriter.OutputListWriter addChild(Consumer<OutputWriter> consumer);
    }

    interface OutputLinkWriter {
        OutputWriter.OutputLinkWriter addLink(String key, String href);

        OutputWriter.OutputLinkWriter addAbsoluteLink(String key, String href);
    }
}
