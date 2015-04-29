/**
 * **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END**********************************
 */

package com.thoughtworks.go.server.database;

import com.thoughtworks.go.util.ArrayUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/* Wrapper around BasicDataSource, which returns WrapperConnection objects when asked
 * for a connection (through getConnection).
 */
public class WrappedDataSource extends BasicDataSource {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("WrappedDataSource");

    private static AtomicLong count = new AtomicLong(0);
    private Map<WrappedConnection, String> connections;

    public WrappedDataSource() {
        super();
        connections = Collections.synchronizedMap(new WeakHashMap<WrappedConnection, String>());
        LOGGER.debug("WRAPPED_SOURCE_CREATED: {}", ArrayUtil.join(Thread.currentThread().getStackTrace(), "\n  "));
    }

    public Map<WrappedConnection, String> getConnections() {
        return connections;
    }

    @Override
    public Connection getConnection() throws SQLException {
        long id = count.incrementAndGet();
        WrappedConnection connection = new WrappedConnection(this, id, super.getConnection());
        connections.put(connection, null);
        return connection;
    }

    @Override
    public Connection getConnection(String user, String pass) throws SQLException {
        long id = count.incrementAndGet();
        WrappedConnection connection = new WrappedConnection(this, id, super.getConnection(user, pass));
        connections.put(connection, null);
        return connection;
    }

    public void removeConnection(WrappedConnection wrappedConnection) {
        connections.remove(wrappedConnection);
    }
}
