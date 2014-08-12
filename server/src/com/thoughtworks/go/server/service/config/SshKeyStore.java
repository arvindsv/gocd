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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.SshKey;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Component
public class SshKeyStore {
    public static final String SSH_KEYS_FILE = "go.ssh.keys.json";
    private final Gson gson = new Gson();
    private SystemEnvironment systemEnvironment;

    // TODO: Use GoCache instead.
    private final Object lockForKey = new Object();
    private List<SshKey> keysCache = null;
    private String cachedChecksum = null;

    @Autowired
    public SshKeyStore(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public List<SshKey> all() {
        synchronized (lockForKey) {
            loadFromCacheIfNeeded();
            return keysCache;
        }
    }

    public SshKey add(String id, String name, String hostname, String username, String key, String resources) {
        synchronized (lockForKey) {
            loadFromCacheIfNeeded();

            SshKey keyToBeAdded = new SshKey(id, name, hostname, username, key, resources);
            keysCache.add(keyToBeAdded);

            syncStorageWithCache();
            return keyToBeAdded;
        }
    }

    public boolean hasKey(String id) {
        synchronized (lockForKey) {
            loadFromCacheIfNeeded();

            for (SshKey sshKey : keysCache) {
                if (sshKey.getId().equals(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    public SshKey deleteKey(String id) {
        synchronized (lockForKey) {
            loadFromCacheIfNeeded();

            int indexOfKey = findIndexOfKeyWithId(id);

            if (indexOfKey < 0) {
                throw new RuntimeException("Cannot find key with ID: " + id);
            }

            SshKey deletedKey = keysCache.remove(indexOfKey);
            syncStorageWithCache();
            return deletedKey;
        }
    }

    public SshKey updateKey(String id, String name, String host, String user, String resources) {
        synchronized (lockForKey) {
            loadFromCacheIfNeeded();

            int indexOfKey = findIndexOfKeyWithId(id);

            if (indexOfKey < 0) {
                throw new RuntimeException("Cannot find key with ID: " + id);
            }

            SshKey updatedKey = keysCache.remove(indexOfKey);
            SshKey replacementKey = new SshKey(id, name, host, user, updatedKey.getKey(), resources);

            keysCache.add(indexOfKey, replacementKey);
            syncStorageWithCache();

            return replacementKey;
        }
    }

    public String checksum() {
        if (cachedChecksum == null) {
            synchronized (lockForKey) {
                loadFromCacheIfNeeded();
                try {
                    cachedChecksum = DigestUtils.sha256Hex(FileUtils.readFileToString(sshKeysFile()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return cachedChecksum;
    }

    private List<SshKey> readFromConfig() throws Exception {
        return gson.fromJson(new FileReader(sshKeysFile()), new TypeToken<List<SshKey>>() { }.getType());
    }

    private void writeToConfig(List<SshKey> keys) {
        try {
            FileUtils.writeStringToFile(sshKeysFile(), gson.toJson(keys));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFromCacheIfNeeded() {
        if (keysCache == null) {
            try {
                keysCache = readFromConfig();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void syncStorageWithCache() {
        List<SshKey> keysCacheToWriteToStorage = keysCache;
        keysCache = null;
        cachedChecksum = null;
        writeToConfig(keysCacheToWriteToStorage);
        loadFromCacheIfNeeded();
    }

    private File sshKeysFile() throws IOException {
        File sshKeysFile = new File(systemEnvironment.getConfigDir(), SSH_KEYS_FILE);
        if (!sshKeysFile.exists()) {
            FileUtils.writeStringToFile(sshKeysFile, "[]");
        }
        return sshKeysFile;
    }

    private int findIndexOfKeyWithId(String id) {
        int indexOfKey = -1;
        for (int i = 0; i < keysCache.size(); i++) {
            if (keysCache.get(i).getId().equals(id)) {
                indexOfKey = i;
            }
        }
        return indexOfKey;
    }
}
