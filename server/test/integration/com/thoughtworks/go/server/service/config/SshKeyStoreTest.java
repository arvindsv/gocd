package com.thoughtworks.go.server.service.config;

import com.thoughtworks.go.config.SshKey;
import com.thoughtworks.go.helper.SshKeyMother;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SshKeyStoreTest {
    private File tempFolder;
    private SystemEnvironment systemEnvironment;
    private SshKeyStore keyStore;

    @Before
    public void setUp() throws Exception {
        tempFolder = TestFileUtil.createUniqueTempFolder("ssh-key-store-test");
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getConfigDir()).thenReturn(tempFolder.getAbsolutePath());

        keyStore = new SshKeyStore(systemEnvironment);
    }

    @After
    public void tearDown() throws Exception {
        TestFileUtil.cleanTempFiles();
    }

    @Test
    public void shouldCreateFileToStoreKeysIfNotFound() throws Exception {
        List<SshKey> allKeys = keyStore.all();

        assertThat(allKeys.size(), is(0));
        assertThat(sshKeysFile().exists(), is(true));
        assertThat(FileUtils.readFileToString(sshKeysFile()), is("[]"));
    }

    private File sshKeysFile() {
        return new File(tempFolder, SshKeyStore.SSH_KEYS_FILE);
    }

    @Test
    public void shouldAddAKey() throws Exception {
        SshKey key = SshKeyMother.aKey(1);

        SshKey addedKey = keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());

        assertThat(keyStore.all().size(), is(1));
        assertThat(keyStore.all().get(0), is(key));

        String contentsOfStorage = FileUtils.readFileToString(sshKeysFile());
        assertThat(contentsOfStorage, containsString(key.getId()));
    }

    @Test
    public void shouldKnowWhenItHasAKeyBasedOnId() throws Exception {
        SshKey key = SshKeyMother.aKey(1);
        keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());

        assertThat(keyStore.hasKey(key.getId()), is(true));
        assertThat(keyStore.hasKey("SOME-NONEXISTENT-ID"), is(false));
    }

    @Test
    public void shouldRemoveAKey() throws Exception {
        SshKey key = SshKeyMother.aKey(1);
        SshKey theKeyToBeDeleted = keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());

        SshKey deletedKey = keyStore.deleteKey(key.getId());

        assertThat(keyStore.all().size(), is(0));
        assertThat(deletedKey, is(theKeyToBeDeleted));
        assertThat(FileUtils.readFileToString(sshKeysFile()), is("[]"));
    }

    @Test
    public void shouldFailIfKeyToRemoveCannotBeFound() throws Exception {
        try {
            keyStore.deleteKey("SOME-NONEXISTENT-ID");
            fail("Should have thrown an exception.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Cannot find key with ID: SOME-NONEXISTENT-ID"));
        }
    }

    @Test
    public void shouldUpdateAKey() throws Exception {
        SshKey key = SshKeyMother.aKey(1);
        keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());

        SshKey updatedKey = keyStore.updateKey(key.getId(), "NAME2", "HOST2", "USER2", "RESOURCES2");

        assertThat(keyStore.all().size(), is(1));

        assertThat(updatedKey.getId(), is(key.getId()));
        assertThat(updatedKey.getName(), is("NAME2"));
        assertThat(updatedKey.getHostname(), is("HOST2"));
        assertThat(updatedKey.getUsername(), is("USER2"));
        assertThat(updatedKey.getKey(), is(key.getKey()));
        assertThat(updatedKey.getResources(), is("RESOURCES2"));

        assertThat(FileUtils.readFileToString(sshKeysFile()), containsString("NAME2"));
        assertThat(FileUtils.readFileToString(sshKeysFile()), containsString("USER2"));
        assertThat(FileUtils.readFileToString(sshKeysFile()), containsString(key.getKey()));
    }

    @Test
    public void shouldFailIfKeyToUpdateCannotBeFound() throws Exception {
        try {
            keyStore.deleteKey("SOME-NONEXISTENT-ID");
            fail("Should have thrown an exception.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Cannot find key with ID: SOME-NONEXISTENT-ID"));
        }
    }

    @Test
    public void shouldBeThreadSafe() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        Future<Object> adder1 = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (int i = 0; i < 100; i++) {
                    SshKey key = SshKeyMother.aKey(i);
                    keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());
                }
                return null;
            }
        });

        Future<Object> adder2 = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (int i = 101; i < 200; i++) {
                    SshKey key = SshKeyMother.aKey(i);
                    keyStore.add(key.getId(), key.getName(), key.getHostname(), key.getUsername(), key.getKey(), key.getResources());
                }
                return null;
            }
        });

        Future<Object> deletor1 = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception{
                for (int i = 0; i < 100; i++) {
                    SshKey key = SshKeyMother.aKey(i);

                    for (int j = 0; j < 100; j++) {
                        if (!keyStore.hasKey(key.getId())) {
                            safeSleep(20);
                        }
                    }
                    keyStore.deleteKey(key.getId());
                }
                return null;
            }

            private void safeSleep(int numberOfMilliseconds) {
                try {
                    Thread.sleep(numberOfMilliseconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertNull(adder1.get());
        assertNull(adder2.get());
        assertNull(deletor1.get());
    }
}