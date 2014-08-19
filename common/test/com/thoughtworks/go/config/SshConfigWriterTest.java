package com.thoughtworks.go.config;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SshConfigWriterTest {
    private SystemEnvironment systemEnvironment;
    private SshConfigWriter configWriter;
    private File folder;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        configWriter = new SshConfigWriter(systemEnvironment);
        folder = TestFileUtil.createUniqueTempFolder("abc");
    }

    @After
    public void tearDown() throws Exception {
        TestFileUtil.cleanTempFiles();
    }

    @Test
    public void shouldUseGoSSHEnvironmentVariableAsFirstChoiceOfSshDirectory() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenReturn("go-ssh-1");
        when(systemEnvironment.getEnvironmentVariable("HOME")).thenReturn("go-home-1");
        when(systemEnvironment.getPropertyImpl("user.home")).thenReturn("go-java-home-1");

        assertThat(configWriter.determineSshDirectory(), is("go-ssh-1"));
    }

    @Test
    public void shouldUseHOMEEnvironmentVariableForCalculatingSshDirectoryWhenGoSSHEnvVarIsNotSet() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenReturn(null);
        when(systemEnvironment.getEnvironmentVariable("HOME")).thenReturn("go-home-1");
        when(systemEnvironment.getPropertyImpl("user.home")).thenReturn("go-java-home-1");

        assertThat(configWriter.determineSshDirectory(), is(new File("go-home-1", ".ssh").getAbsolutePath()));
    }

    @Test
    public void shouldUseJavaHomeSystemPropertyForCalculatingSshDirectoryWhenOthersAreNotSet() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenReturn(null);
        when(systemEnvironment.getEnvironmentVariable("HOME")).thenReturn(null);
        when(systemEnvironment.getPropertyImpl("user.home")).thenReturn("go-java-home-1");

        assertThat(configWriter.determineSshDirectory(), is(new File("go-java-home-1", ".ssh").getAbsolutePath()));
    }

    @Test
    public void shouldUseCurrentDirectoryAsJavaHomeIfNothingElseWorks() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenReturn(null);
        when(systemEnvironment.getEnvironmentVariable("HOME")).thenReturn(null);
        when(systemEnvironment.getPropertyImpl("user.home")).thenReturn(null);

        assertThat(configWriter.determineSshDirectory(), is(new File(".", ".ssh").getAbsolutePath()));
    }

    @Test
    public void shouldUseCurrentDirectoryAsJavaHomeIfSomethingFails() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenThrow(new RuntimeException("Ouch"));

        assertThat(configWriter.determineSshDirectory(), is(new File(".", ".ssh").getAbsolutePath()));
    }

    @Test
    public void shouldCacheSSHDirectoryDetermined() throws Exception {
        when(systemEnvironment.getEnvironmentVariable("GO_SSH_DIR")).thenReturn(null);

        String directoryForFirstRun = configWriter.determineSshDirectory();
        String directoryForSecondRun = configWriter.determineSshDirectory();

        assertThat(directoryForFirstRun, is(directoryForSecondRun));
        verify(systemEnvironment, times(1)).getEnvironmentVariable("GO_SSH_DIR");
    }

    @Test
    public void shouldNotTryAndCreateSSHConfigFileIfItExists() throws Exception {
        File existingDirectory = folder;
        File configFile = new File(existingDirectory, "config");
        FileUtils.writeStringToFile(configFile, "SOME-STUFF");

        configWriter.createSSHConfigFile(existingDirectory);
        assertThat(FileUtils.readFileToString(configFile), is("SOME-STUFF"));
    }

    @Test
    public void shouldCreateSSHConfigFileWithCorrectPermissionsIfItDoesNotExist() throws Exception {
        File existingDirectory = folder;

        configWriter.createSSHConfigFile(existingDirectory);

        File config = new File(existingDirectory, "config");
        assertThat(FileUtils.readFileToString(config), is(""));
        assertThat(config.canRead(), is(true));
        assertThat(config.canWrite(), is(true));
        assertThat(config.canExecute(), is(false));
    }

    @Test
    public void shouldCreateSSHConfigDirectoryWithCorrectPermissionsIfItDoesNotExist() throws Exception {
        File nonExistentDirectory = new File(folder, "some-random-dir");

        configWriter.createSSHConfigFile(nonExistentDirectory);

        assertThat(nonExistentDirectory.canRead(), is(true));
        assertThat(nonExistentDirectory.canWrite(), is(true));
        assertThat(nonExistentDirectory.canExecute(), is(true));

        File config = new File(nonExistentDirectory, "config");
        assertThat(config.canRead(), is(true));
        assertThat(config.canWrite(), is(true));
        assertThat(config.canExecute(), is(false));
    }

    @Test
    public void shouldFailIfDirectoryCannotBeCreated() throws Exception {
        File anExistingFile = TestFileUtil.createUniqueTempFile("some-prefix");
        anExistingFile.createNewFile();

        try {
            configWriter.createSSHConfigFile(anExistingFile);
            fail("Should have thrown an exception.");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to create SSH config file in:"));
        }
    }
}