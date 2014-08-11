/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

public class SshKey {
    private final String id;
    private final String name;
    private final String hostname;
    private final String username;
    private final String key;
    private final String resources;

    public SshKey(String id, String name, String hostname, String username, String key, String resources) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.username = username;
        this.key = key;
        this.resources = resources;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    public String getResources() {
        return resources;
    }
}
