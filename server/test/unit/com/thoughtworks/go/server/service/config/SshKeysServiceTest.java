package com.thoughtworks.go.server.service.config;

import com.thoughtworks.go.config.SshKey;
import com.thoughtworks.go.config.validation.ValidationError;
import com.thoughtworks.go.helper.SshKeyMother;
import com.thoughtworks.go.server.util.UuidGenerator;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SshKeysServiceTest {
    private SshKeysService service;
    private UuidGenerator uuidGenerator;
    private SshKeyStore keyStore;

    @Before
    public void setUp() throws Exception {
        uuidGenerator = mock(UuidGenerator.class);
        keyStore = mock(SshKeyStore.class);
        service = new SshKeysService(uuidGenerator, keyStore);
    }

    @Test
    public void shouldAddANewKey() throws Exception {
        SshKey expectedKeyToBeAdded = SshKeyMother.key("SOME-ID-1", "NAME1", "HOST1", "USER1", "KEY1", "RESOURCES1");

        when(uuidGenerator.randomUuid()).thenReturn("SOME-ID-1");
        when(keyStore.add("SOME-ID-1", "NAME1", "HOST1", "USER1", "KEY1", "RESOURCES1")).thenReturn(expectedKeyToBeAdded);

        SshKey newKey = service.addKey("NAME1", "HOST1", "USER1", "KEY1", "RESOURCES1");

        assertThat(newKey, is(expectedKeyToBeAdded));
    }

    @Test
    public void shouldRetrieveAllKeys() throws Exception {
        SshKey key1 = SshKeyMother.key("SOME-ID-1", "NAME1", "HOST1", "USER1", "KEY1", "RESOURCES1");
        SshKey key2 = SshKeyMother.key("SOME-ID-2", "NAME2", "HOST2", "USER2", "KEY2", "RESOURCES2");

        List<SshKey> expectedKeys = asList(key1, key2);
        when(keyStore.all()).thenReturn(expectedKeys);

        List<SshKey> allKeys = service.all();

        assertThat(allKeys, is(expectedKeys));
    }

    @Test
    public void shouldDelegateToKeyStoreForKeyExistanceCheck() throws Exception {
        when(keyStore.hasKey("ID1")).thenReturn(true);
        when(keyStore.hasKey("ID2")).thenReturn(false);

        assertThat(service.hasKey("ID1"), is(true));
        assertThat(service.hasKey("ID2"), is(false));
    }

    @Test
    public void shouldDelegateToKeyStoreForKeyDeletion() throws Exception {
        SshKey key = SshKeyMother.key("SOME-ID-1", "NAME1", "HOST1", "USER1", "KEY1", "RESOURCES1");

        when(keyStore.deleteKey("ID1")).thenReturn(key);

        assertThat(service.deleteKey("ID1"), is(key));
    }

    @Test
    public void shouldDelegateToKeyStoreForKeyUpdation() throws Exception {
        SshKey key = SshKeyMother.key("SOME-ID-1", "NAME2", "HOST2", "USER2", "KEY2", "RESOURCES2");

        when(keyStore.updateKey("SOME-ID-1", "NAME2", "HOST2", "USER2", "RESOURCES2")).thenReturn(key);

        assertThat(service.updateKey("SOME-ID-1", "NAME2", "HOST2", "USER2", "RESOURCES2"), is(key));
    }

    @Test
    public void shouldFailValidationIfNameOrHostOrKeyAreEmpty() throws Exception {
        assertThat(service.validate(null, "HOST1", "USER1", "KEY1", "RESOURCES1"), is(asList(errorOn("name", "Should not be empty"))));
        assertThat(service.validate("", "HOST1", "USER1", "KEY1", "RESOURCES1"), is(asList(errorOn("name", "Should not be empty"))));

        assertThat(service.validate("NAME1", null, "USER1", "KEY1", "RESOURCES1"), is(asList(errorOn("hostname", "Should not be empty"))));
        assertThat(service.validate("NAME1", "", "USER1", "KEY1", "RESOURCES1"), is(asList(errorOn("hostname", "Should not be empty"))));

        assertThat(service.validate("NAME1", "HOST1", "USER1", null, "RESOURCES1"), is(asList(errorOn("key", "Should not be empty"))));
        assertThat(service.validate("NAME1", "HOST1", "USER1", "", "RESOURCES1"), is(asList(errorOn("key", "Should not be empty"))));
    }

    @Test
    public void shouldSucceedValidationEvenIfUsernameOrResourcesAreEmpty() throws Exception {
        assertThat(service.validate("NAME1", "HOST1", null, "KEY1", "RESOURCES1"), is(empty()));
        assertThat(service.validate("NAME1", "HOST1", "", "KEY1", "RESOURCES1"), is(empty()));

        assertThat(service.validate("NAME1", "HOST1", "USER1", "KEY1", null), is(empty()));
        assertThat(service.validate("NAME1", "HOST1", "USER1", "KEY1", ""), is(empty()));
    }

    @Test
    public void shouldValidateOnlyNameAndHostDuringUpdateValidation() throws Exception {
        assertThat(service.validateUpdate("ID1", null, "HOST1", "USER1", "RESOURCES1"), is(asList(errorOn("name", "Should not be empty"))));
        assertThat(service.validateUpdate("ID1", "", "HOST1", "USER1", "RESOURCES1"), is(asList(errorOn("name", "Should not be empty"))));

        assertThat(service.validateUpdate("ID1", "NAME1", null, "USER1", "RESOURCES1"), is(asList(errorOn("hostname", "Should not be empty"))));
        assertThat(service.validateUpdate("ID1", "NAME1", "", "USER1", "RESOURCES1"), is(asList(errorOn("hostname", "Should not be empty"))));

        assertThat(service.validateUpdate("ID1", "NAME1", "HOST1", null, "RESOURCES1"), is(empty()));
        assertThat(service.validateUpdate("ID1", "NAME1", "HOST1", "", "RESOURCES1"), is(empty()));

        assertThat(service.validateUpdate("ID1", "NAME1", "HOST1", "USER1", null), is(empty()));
        assertThat(service.validateUpdate("ID1", "NAME1", "HOST1", "USER1", ""), is(empty()));
    }

    @Test
    public void shouldDelegateToKeyStoreForChecksumOfAllKeys() throws Exception {
        when(keyStore.checksum()).thenReturn("ABC");

        assertThat(service.checksum(), is("ABC"));
    }

    private ValidationError errorOn(String key, String value) {
        return new ValidationError(key, value);
    }
}