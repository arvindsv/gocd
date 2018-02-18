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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.thoughtworks.go.spark.RequestContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

public class JsonOutputWriter {
    private final Writer writer;
    private final RequestContext requestContext;

    public JsonOutputWriter(Writer writer, RequestContext requestContext) {
        this.writer = writer;
        this.requestContext = requestContext;
    }

    public void forTopLevelObject(Consumer<OutputWriter> consumer) {
        bufferWriterAndFlushWhenDone(writer, bufferedWriter -> {
            try (JsonOutputWriterUsingJackson jacksonOutputWriter = new JsonOutputWriterUsingJackson(bufferedWriter, requestContext)) {
                jacksonOutputWriter.forTopLevelObject(consumer);
            }
        });
    }

    public void forTopLevelArray(Consumer<OutputWriter.OutputListWriter> listWriterConsumer) {
        bufferWriterAndFlushWhenDone(writer, bufferedWriter -> {
            try (JsonOutputWriterUsingJackson jacksonOutputWriter = new JsonOutputWriterUsingJackson(writer, requestContext)) {
                jacksonOutputWriter.forTopLevelArray(listWriterConsumer);
            }
        });
    }

    private void bufferWriterAndFlushWhenDone(Writer writer, Consumer<BufferedWriter> consumer) {
        BufferedWriter bufferedWriter = (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer);
        try {
            try {
                consumer.accept(bufferedWriter);
            } finally {
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JsonOutputWriterUsingJackson implements OutputWriter {
        private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

        private final RequestContext requestContext;
        private final JsonGenerator jacksonWriter;

        private JsonOutputWriterUsingJackson(Writer writer, RequestContext requestContext) {
            this.requestContext = requestContext;
            try {
                JsonFactory factory = new JsonFactory();
                jacksonWriter = factory.createGenerator(writer);
                jacksonWriter.useDefaultPrettyPrinter();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, String value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeStringField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, String value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, int value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeNumberField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, boolean value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeBooleanField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, long value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeNumberField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, Date value) {
            return withExceptionHandling((jacksonWriter) -> {
                String valueAsString = value == null ? null : ISO8601Utils.format(value, false, UTC);
                jacksonWriter.writeStringField(key, valueAsString);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, Date value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public OutputWriter addChild(String key, Consumer<OutputWriter> consumer) {
            return new JsonOutputChildWriter(key, this).body(consumer);
        }

        @Override
        public OutputWriter addChildList(String key, Consumer<OutputListWriter> listWriterConsumer) {
            return new JsonOutputListWriter(this).body(key, listWriterConsumer);
        }

        @Override
        public OutputWriter addChildList(String key, Collection<String> values) {
            return new JsonOutputListWriter(this).body(key, listWriter -> values.forEach(listWriter::value));
        }

        @Override
        public OutputWriter addLinks(Consumer<OutputLinkWriter> consumer) {
            return withExceptionHandling((jacksonWriter) -> {
                addChild("_links", (childWriter) -> {
                    consumer.accept(new JsonOutputLinkWriter(childWriter));
                });
            });
        }

        private JsonOutputWriterUsingJackson withExceptionHandling(ConsumerWhichThrows consumerWhichThrows) {
            consumerWhichThrows.accept(this.jacksonWriter);
            return this;
        }

        @FunctionalInterface
        interface ConsumerWhichThrows extends Consumer<JsonGenerator> {
            void acceptWhichThrows(JsonGenerator writer) throws Exception;

            @Override
            default void accept(JsonGenerator writer) {
                try {
                    acceptWhichThrows(writer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void forTopLevelObject(Consumer<OutputWriter> consumer) {
            withExceptionHandling(writer -> {
                writer.writeStartObject();
                consumer.accept(this);
                writer.writeEndObject();
            });
        }

        private void forTopLevelArray(Consumer<OutputListWriter> consumer) {
            withExceptionHandling(writer -> {
                new JsonOutputListWriter(this).startArrayWithoutName(consumer);
            });
        }

        @Override
        public void close() {
            if (!this.jacksonWriter.isClosed()) {
                withExceptionHandling(JsonGenerator::close);
            }
        }

        public class JsonOutputChildWriter {
            private String key;
            private JsonOutputWriterUsingJackson parentWriter;

            JsonOutputChildWriter(String key, JsonOutputWriterUsingJackson parentWriter) {
                this.key = key;
                this.parentWriter = parentWriter;
            }

            public JsonOutputWriterUsingJackson body(Consumer<OutputWriter> consumer) {
                return parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeFieldName(key);
                    jacksonWriter.writeStartObject();
                    consumer.accept(parentWriter);
                    jacksonWriter.writeEndObject();
                });
            }
        }


        public class JsonOutputListWriter implements OutputListWriter {
            private final JsonOutputWriterUsingJackson parentWriter;

            JsonOutputListWriter(JsonOutputWriterUsingJackson parentWriter) {
                this.parentWriter = parentWriter;
            }

            private JsonOutputWriterUsingJackson body(String key, Consumer<OutputListWriter> consumer) {
                return parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeFieldName(key);
                    startArrayWithoutName(consumer);
                });
            }

            private void startArrayWithoutName(Consumer<OutputListWriter> consumer) {
                parentWriter.withExceptionHandling(jacksonWriter -> {
                    jacksonWriter.writeStartArray();
                    consumer.accept(this);
                    jacksonWriter.writeEndArray();
                });
            }

            @Override
            public JsonOutputListWriter value(String value) {
                parentWriter.withExceptionHandling((jacksonWriter) -> jacksonWriter.writeString(value));
                return this;
            }

            @Override
            public JsonOutputListWriter addChild(Consumer<OutputWriter> consumer) {
                parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeStartObject();
                    consumer.accept(parentWriter);
                    jacksonWriter.writeEndObject();
                });
                return this;
            }
        }


        public class JsonOutputLinkWriter implements OutputLinkWriter {
            private OutputWriter parentWriter;

            JsonOutputLinkWriter(OutputWriter parentWriter) {
                this.parentWriter = parentWriter;
            }

            @Override
            public JsonOutputLinkWriter addLink(String key, String href) {
                return addAbsoluteLink(key, requestContext.urlFor(href));
            }

            @Override
            public JsonOutputLinkWriter addAbsoluteLink(String key, String href) {
                parentWriter.addChild(key, innerChildWriter -> {
                    innerChildWriter.add("href", href);
                });
                return this;
            }
        }
    }
}
