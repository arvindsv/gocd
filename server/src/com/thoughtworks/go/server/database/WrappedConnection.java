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
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/* This is a wrapper around a java.sql.Connection, which delegates everything, but
 * logs open and close connection times. It also has a getTrace() method, which
 * has a stacktrace of its creation. Useful to find leaks or long-running connections.
 */
public class WrappedConnection implements Connection {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("ConnectionLogger");

    private final String trace;
    private final java.util.Date startTime;
    private WrappedDataSource wrappedDataSource;
    private final Connection wrappedConnection;
    private final long id;

    public WrappedConnection(WrappedDataSource wrappedDataSource, long id, Connection connection) {
        this.wrappedDataSource = wrappedDataSource;
        this.wrappedConnection = connection;

        this.startTime = new java.util.Date();
        this.id = id;
        this.trace = Thread.currentThread().getName() + "\n  " + ArrayUtil.join(Thread.currentThread().getStackTrace(), "\n  ");
        LOGGER.debug("OPEN  {}", id);
    }

    public String getTrace() {
        return id + ": " + startTime + ":  " + trace;
    }

    public Statement createStatement() throws SQLException {
        return wrappedConnection.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return wrappedConnection.prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return wrappedConnection.prepareCall(sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return wrappedConnection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        wrappedConnection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return wrappedConnection.getAutoCommit();
    }

    public void commit() throws SQLException {
        wrappedConnection.commit();
    }

    public void rollback() throws SQLException {
        wrappedConnection.rollback();
    }

    public void close() throws SQLException {
        LOGGER.debug("CLOSE {} - {}", id, TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - startTime.getTime()));
        wrappedConnection.close();
        wrappedDataSource.removeConnection(this);
    }

    public boolean isClosed() throws SQLException {
        return wrappedConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return wrappedConnection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        wrappedConnection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return wrappedConnection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        wrappedConnection.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return wrappedConnection.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        wrappedConnection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return wrappedConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return wrappedConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        wrappedConnection.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return wrappedConnection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        wrappedConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        wrappedConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return wrappedConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return wrappedConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return wrappedConnection.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        wrappedConnection.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        wrappedConnection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return wrappedConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return wrappedConnection.prepareStatement(sql, columnNames);
    }

    public Clob createClob() throws SQLException {
        return wrappedConnection.createClob();
    }

    public Blob createBlob() throws SQLException {
        return wrappedConnection.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return wrappedConnection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return wrappedConnection.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return wrappedConnection.isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        wrappedConnection.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        wrappedConnection.setClientInfo(properties);
    }

    public String getClientInfo(String name) throws SQLException {
        return wrappedConnection.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        return wrappedConnection.getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return wrappedConnection.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return wrappedConnection.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        wrappedConnection.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return wrappedConnection.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        wrappedConnection.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        wrappedConnection.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return wrappedConnection.getNetworkTimeout();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return wrappedConnection.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return wrappedConnection.isWrapperFor(iface);
    }
}
