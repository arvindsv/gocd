package com.thoughtworks.go.remote.communication;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GoAgentServerCommunicationTypeValidationTest {
    private GoAgentServerCommunicationTypeValidation validation;

    @Before
    public void setUp() throws Exception {
        validation = new GoAgentServerCommunicationTypeValidation();
    }

    @Test
    public void shouldAllowSomeBasicTypesAsValidClasses() throws Exception {
        isValid(String.class);
        isValid(Boolean.class);
        isValid(RuntimeException.class);
    }

    @Test
    public void shouldConsiderAnyClassWhichHasTheAnnotationInItsHierarchyAsValid() throws Exception {
        isValid(TopLevelInterfaceWithAnnotation.class);
        isInvalid(TopLevelInterfaceWithoutAnnotation.class);

        isValid(TopLevelClassWithAnnotation.class);
        isInvalid(TopLevelClassWithoutAnnotation.class);

        isValid(Level2DescendentOfClassWithAnnotation.class);
        isInvalid(Level2DescendentOfClassWithoutAnnotation.class);

        isValid(Level2DescendentOfInterfaceWithAnnotation.class);
        isValid(Level2DescendentOfInterfaceWithDuplicateAnnotation.class);
        isInvalid(Level2DescendentOfInterfaceWithoutAnnotation.class);

        isValid(Level3DescendentOfClassWithAnnotation.class);
        isInvalid(Level3DescendentOfClassWithoutAnnotation.class);

        isValid(Level3DescendentOfInterfaceWithAnnotation.class);
        isInvalid(Level3DescendentOfInterfaceWithoutAnnotation.class);

        isValid(Level3DescendentWhichImplementsMultipleInterfacesOneOfWhichHasAnnotation.class);
        isInvalid(Level3DescendentWhichImplementsMultipleInterfaces_No_One_OfWhichHasAnnotation.class);
    }

    private void isValid(Class<?> clazz) {
        assertTrue("Should have been valid: " + clazz, validation.isValid(clazz));
    }

    private void isInvalid(Class<?> clazz) {
        assertFalse("Should have been invalid: " + clazz, validation.isValid(clazz));
    }
}

@AllowInSerializationBetweenAgentAndServer
interface TopLevelInterfaceWithAnnotation {}

interface TopLevelInterfaceWithoutAnnotation {}

interface SomeOtherInterfaceWithNoAnnotation {}

@AllowInSerializationBetweenAgentAndServer
class TopLevelClassWithAnnotation {}

class TopLevelClassWithoutAnnotation {}

class Level2DescendentOfClassWithAnnotation extends TopLevelClassWithAnnotation {}

class Level2DescendentOfClassWithoutAnnotation extends TopLevelClassWithoutAnnotation {}

class Level2DescendentOfInterfaceWithAnnotation implements TopLevelInterfaceWithAnnotation {}

@AllowInSerializationBetweenAgentAndServer
class Level2DescendentOfInterfaceWithDuplicateAnnotation implements TopLevelInterfaceWithAnnotation {}

class Level2DescendentOfInterfaceWithoutAnnotation implements TopLevelInterfaceWithoutAnnotation {}

class Level3DescendentOfClassWithAnnotation extends Level2DescendentOfClassWithAnnotation {}

class Level3DescendentOfClassWithoutAnnotation extends Level2DescendentOfClassWithoutAnnotation {}

class Level3DescendentOfInterfaceWithAnnotation extends Level2DescendentOfInterfaceWithAnnotation {}

class Level3DescendentOfInterfaceWithoutAnnotation extends Level2DescendentOfInterfaceWithoutAnnotation {}

class Level3DescendentWhichImplementsMultipleInterfacesOneOfWhichHasAnnotation extends Level2DescendentOfInterfaceWithoutAnnotation implements TopLevelInterfaceWithAnnotation, TopLevelInterfaceWithoutAnnotation {}

class Level3DescendentWhichImplementsMultipleInterfaces_No_One_OfWhichHasAnnotation extends Level2DescendentOfInterfaceWithoutAnnotation implements TopLevelInterfaceWithoutAnnotation, SomeOtherInterfaceWithNoAnnotation {}