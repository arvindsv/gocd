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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

public class SshConfigWriter {
    private SystemEnvironment systemEnvironment;
    private static final Log LOG = LogFactory.getLog(SshConfigWriter.class);
    private String cachedSshDirectory;

    public SshConfigWriter(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public String determineSshDirectory() {
        if (cachedSshDirectory != null) {
            return cachedSshDirectory;
        }

        String directory = new File(".", ".ssh").getAbsolutePath();
        try {
            if (systemEnvironment.getEnvironmentVariable("GO_SSH_DIR") != null) {
                directory = systemEnvironment.getEnvironmentVariable("GO_SSH_DIR");
            }
            else if (systemEnvironment.getEnvironmentVariable("HOME") != null) {
                directory = new File(systemEnvironment.getEnvironmentVariable("HOME"), ".ssh").getAbsolutePath();
            }
            else if (systemEnvironment.getPropertyImpl("user.home") != null) {
                directory = new File(systemEnvironment.getPropertyImpl("user.home"), ".ssh").getAbsolutePath();
            }
        } catch (Exception e) {
            LOG.error("Failed to determine home directory: " + e.getMessage() + ". Reverting to using current directory.", e);
        }

        cachedSshDirectory = directory;
        return cachedSshDirectory;
    }

    public void createSSHConfigFile(File sshConfigDirectory) {
        try {
            if (!sshConfigDirectory.exists()) {
                sshConfigDirectory.mkdirs();
                setRWX(sshConfigDirectory, true, true, true);
            }

            File configFile = new File(sshConfigDirectory, "config");
            if (!configFile.exists()) {
                configFile.createNewFile();
                setRWX(configFile, true, true, false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SSH config file in: " + sshConfigDirectory, e);
        }
    }

    /* Convoluted Java way of setting permissions to 600 or 700. Set everything to 0. Then, set what you want. */
    private void setRWX(File configFile, boolean readable, boolean writeable, boolean executable) {
        configFile.setReadable(false, false);
        configFile.setWritable(false, false);
        configFile.setExecutable(false, false);
        configFile.setReadable(readable, true);
        configFile.setWritable(writeable, true);
        configFile.setExecutable(executable, true);
    }
}
