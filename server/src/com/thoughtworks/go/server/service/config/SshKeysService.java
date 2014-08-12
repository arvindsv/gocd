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

package com.thoughtworks.go.server.service.config;

import com.thoughtworks.go.config.SshKey;
import com.thoughtworks.go.config.validation.ValidationError;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SshKeysService {
    private UuidGenerator uuidGenerator;
    private SshKeyStore keyStore;

    @Autowired
    public SshKeysService(UuidGenerator uuidGenerator, SshKeyStore keyStore) {
        this.uuidGenerator = uuidGenerator;
        this.keyStore = keyStore;
    }

    public List<SshKey> all() {
        return keyStore.all();
    }

    public List<ValidationError> validate(String name, String hostname, String username, String key, String resources) {
        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
        ensureNotEmpty(errors, "name", name);
        ensureNotEmpty(errors, "hostname", hostname);
        ensureNotEmpty(errors, "key", key);
        return errors;
    }

    public SshKey addKey(String name, String hostname, String username, String key, String resources) {
        return keyStore.add(uuidGenerator.randomUuid(), name, hostname, username, key, resources);
    }

    public boolean hasKey(String id) {
        return keyStore.hasKey(id);
    }

    public List<ValidationError> validateUpdate(String id, String name, String hostname, String username, String resources) {
        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
        ensureNotEmpty(errors, "name", name);
        ensureNotEmpty(errors, "hostname", hostname);
        return errors;
    }

    public SshKey updateKey(String id, String name, String hostname, String username, String resources) {
        return keyStore.updateKey(id, name, hostname, username, resources);
    }

    public SshKey deleteKey(String id) {
        return keyStore.deleteKey(id);
    }

    private void ensureNotEmpty(List<ValidationError> currentErrors, String keyName, String keyValue) {
        if (StringUtil.isBlank(keyValue)) {
            currentErrors.add(new ValidationError(keyName, "Should not be empty"));
        }
    }

    public String checksum() {
        return keyStore.checksum();
    }
}
