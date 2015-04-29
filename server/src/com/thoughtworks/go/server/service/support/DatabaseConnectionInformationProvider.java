/**
 * **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END**********************************
 */

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.database.WrappedConnection;
import com.thoughtworks.go.server.database.WrappedDataSource;
import com.thoughtworks.go.server.util.DatabaseUpgraderDataSourceFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
public class DatabaseConnectionInformationProvider implements ServerInfoProvider {
    private DatabaseUpgraderDataSourceFactory dataSourceFactory;

    @Autowired
    public DatabaseConnectionInformationProvider(DatabaseUpgraderDataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection("Database connection information");

        DataSource dataSource = dataSourceFactory.dataSource();

        if (dataSource instanceof WrappedDataSource) {
            enhancedInformation((WrappedDataSource) dataSource, infoCollector);
        } else if (dataSource instanceof BasicDataSource) {
            basicInformation((BasicDataSource) dataSource, infoCollector);
        }
    }

    private void basicInformation(BasicDataSource dataSource, InformationStringBuilder infoCollector) {
        infoCollector.append("Active connections: ").append(dataSource.getNumActive()).append(" out of a max of ").append(dataSource.getMaxActive()).append("\n");
        infoCollector.append("Idle connections: ").append(dataSource.getNumIdle()).append("\n");
    }

    private void enhancedInformation(WrappedDataSource dataSource, InformationStringBuilder infoCollector) {
        basicInformation(dataSource, infoCollector);

        infoCollector.addSubSection("Current open connections");

        long startTime = System.nanoTime();
        Map<WrappedConnection, String> connectionsMap = dataSource.getConnections();
        Set<WrappedConnection> connections = connectionsMap.keySet();
        synchronized (connectionsMap) {
            Iterator i = connections.iterator();
            while (i.hasNext()) {
                WrappedConnection connection = (WrappedConnection) i.next();
                if (connection != null) {
                    infoCollector.append(connection.getTrace()).append("\n\n");
                }
            }
        }

        infoCollector.append("Time in sync block for listing connections (nanoseconds): ").append(System.nanoTime() - startTime);
    }

    @Override
    public double priority() {
        return 7.0;
    }
}
