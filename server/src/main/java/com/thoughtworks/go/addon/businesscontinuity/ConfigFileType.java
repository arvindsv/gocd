/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.addon.businesscontinuity;

import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;


public enum ConfigFileType {
    CRUISE_CONFIG_XML(systemEnvironment -> {
        return new File(systemEnvironment.getCruiseConfigFile());
    }),

    DES_CIPHER(SystemEnvironment::getDESCipherFile),

    AES_CIPHER(SystemEnvironment::getAESCipherFile),

    JETTY_XML(SystemEnvironment::getJettyConfigFile),

    USER_FEATURE_TOGGLE(systemEnvironment -> {
        return new File(systemEnvironment.getConfigDir(), systemEnvironment.get(SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR));
    });

    private final ConfigFileOperations configFileOperations;

    ConfigFileType(ConfigFileOperations configFileOperations) {
        this.configFileOperations = configFileOperations;
    }

    public File load(SystemEnvironment systemEnvironment) {
        return configFileOperations.load(systemEnvironment);
    }
}

interface ConfigFileOperations {
    File load(SystemEnvironment systemEnvironment);
}