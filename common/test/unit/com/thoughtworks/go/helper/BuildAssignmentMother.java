package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BuildAssignmentMother {
    private static final String BUILDERS_KEY = "builders";
    private static final String BUILD_CAUSE_KEY = "buildCause";
    private HashMap<String, Object> contextOfThisMother;

    private BuildAssignmentMother(HashMap<String, Object> contextOfThisMother) {
        this.contextOfThisMother = contextOfThisMother;
        this.contextOfThisMother.put(BUILDERS_KEY, new ArrayList<Builder>());
        this.contextOfThisMother.put(BUILD_CAUSE_KEY, BuildCause.createManualForced(MaterialRevisions.EMPTY, Username.ANONYMOUS));
    }

    public static BuildAssignmentMother start() {
        return new BuildAssignmentMother(new HashMap<String, Object>());
    }

    public BuildAssignment done() {
        Resources resources = new Resources("abc, def");
        ArtifactPlans artifactPlans = new ArtifactPlans(Arrays.asList(new ArtifactPlan("src1", "dest1"), new TestArtifactPlan("src2", "dest2")));
        ArtifactPropertiesGenerators properties = new ArtifactPropertiesGenerators(Arrays.asList(new ArtifactPropertiesGenerator("art1", "src3", "xpath1")));
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline1", 1, "label1", "stage1", "1234", "job1");

        List<Builder> builders = (List<Builder>) contextOfThisMother.get(BUILDERS_KEY);
        BuildCause buildCause = (BuildCause) contextOfThisMother.get(BUILD_CAUSE_KEY);
        return BuildAssignment.create(new DefaultJobPlan(resources, artifactPlans, properties, 123, jobIdentifier), buildCause, builders, new File("whatisthis"));
    }

    public BuildAssignmentMother addBuilder(Builder builder) {
        ((List<Builder>) contextOfThisMother.get(BUILDERS_KEY)).add(builder);
        return this;
    }

    public BuildAssignmentMother withBuildCause(BuildCause buildCause) {
        contextOfThisMother.put(BUILD_CAUSE_KEY, buildCause);
        return this;
    }
}
