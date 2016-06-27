/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigPipelinePermissionsAuthorityTest {
    private GoConfigMother configMother;
    private GoConfigService configService;
    private GoConfigPipelinePermissionsAuthority service;
    private CruiseConfig config;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        service = new GoConfigPipelinePermissionsAuthority(configService);

        configMother = new GoConfigMother();
        config = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldConsiderAllSuperAdminUsersAsViewersOperatorsAndAdminsOfPipelines() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addUserAsSuperAdmin(config, "superadmin2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", "superadmin1", "superadmin2", "viewer1");
        assertOperators(permissions, "pipeline1", "superadmin1", "superadmin2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "superadmin2");
    }

    @Test
    public void shouldConsiderUsersOfAllSuperAdminRolesAsViewersOperatorsAndAdminsOfPipelines() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRole(config, configMother.createRole("superadminrole2", "superadmin2", "superadmin3"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");
        configMother.addRoleAsSuperAdmin(config, "superadminrole2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", "superadmin1", "superadmin2", "superadmin3", "viewer1");
        assertOperators(permissions, "pipeline1", "superadmin1", "superadmin2", "superadmin3");
        assertAdmins(permissions, "pipeline1", "superadmin1", "superadmin2", "superadmin3");
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothSuperAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", "superadmin1", "superadmin2", "viewer1");
        assertOperators(permissions, "pipeline1", "superadmin1", "superadmin2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "superadmin2");
    }

    @Test
    public void shouldConsiderPipelineGroupAdminsAsViewersOperatorsAndAdminsOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");
        assertOperators(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");

        assertViewers(permissions, "pipeline2", "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", "superadmin1");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldConsiderAllUsersInPipelineGroupAdminRolesAsViewersOperatorsAndAdminsOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addRole(config, configMother.createRole("group1_admin_role2", "groupadmin2", "groupadmin3"));
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");
        assertOperators(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");
        assertAdmins(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");

        assertViewers(permissions, "pipeline2", "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", "superadmin1");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothGroupAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");
        assertOperators(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "groupadmin1", "groupadmin2");
    }

    @Test
    public void shouldConsiderUsersWithViewPermissionsAsOnlyViewersOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer3", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1", "viewer1", "viewer2");
        assertOperators(permissions, "pipeline1", "superadmin1");
        assertAdmins(permissions, "pipeline1", "superadmin1");

        assertViewers(permissions, "pipeline2", "superadmin1", "viewer3");
        assertOperators(permissions, "pipeline2", "superadmin1");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldConsiderUsersOfRolesWithViewPermissionsAsOnlyViewersOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "groupviewer1", "groupviewer2"));
        configMother.addRole(config, configMother.createRole("group1_view_role2", "groupviewer2", "groupviewer3"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1", "groupviewer1", "groupviewer2", "groupviewer3");
        assertOperators(permissions, "pipeline1", "superadmin1");
        assertAdmins(permissions, "pipeline1", "superadmin1");

        assertViewers(permissions, "pipeline2", "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", "superadmin1");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldConsiderUsersWithOperatePermissionsAsOnlyOperatorsOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator2", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator3", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1");
        assertOperators(permissions, "pipeline1", "superadmin1", "operator1", "operator2");
        assertAdmins(permissions, "pipeline1", "superadmin1");

        assertViewers(permissions, "pipeline2", "superadmin1");
        assertOperators(permissions, "pipeline2", "superadmin1", "operator3");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldConsiderUsersOfRolesWithOperatePermissionsAsOnlyOperatorsOfTheirPipelines() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group2");

        configMother.addRole(config, configMother.createRole("group1_operate_role1", "groupoperator1", "groupoperator2"));
        configMother.addRole(config, configMother.createRole("group1_operate_role2", "groupoperator2", "groupoperator3"));
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group1_operate_role1", "group1");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group1_operate_role2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", "superadmin1");
        assertOperators(permissions, "pipeline1", "superadmin1", "groupoperator1", "groupoperator2", "groupoperator3");
        assertAdmins(permissions, "pipeline1", "superadmin1");

        assertViewers(permissions, "pipeline2", "superadmin1");
        assertOperators(permissions, "pipeline2", "superadmin1", "operator1");
        assertAdmins(permissions, "pipeline2", "superadmin1");
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothViewUsersAndViewRolesConfigurations() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "viewer1", "groupviewer2"));
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertViewers(permissions, "pipeline1", "superadmin1", "viewer1", "groupviewer2");
        assertOperators(permissions, "pipeline1", "superadmin1");
        assertAdmins(permissions, "pipeline1", "superadmin1");
    }

    @Test
    public void shouldCreateAnEntryForEveryPipelineInTheConfig() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline3", "stage1C", "job1C1", "job1C2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline4", "stage1D", "job1D1", "job1D2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "group1admin1", "group1admin2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "group2viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "group3viewer1", "group3viewer2"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");
        configMother.addUserAsOperatorOfPipelineGroup(config, "group3operator1", "group3");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2", "pipeline3", "pipeline4");

        assertViewers(permissions, "pipeline1", "superadmin1", "group1admin1", "group1admin2");
        assertOperators(permissions, "pipeline1", "superadmin1", "group1admin1", "group1admin2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "group1admin1", "group1admin2");

        assertViewers(permissions, "pipeline2", "superadmin1", "group2viewer1");
        assertOperators(permissions, "pipeline2", "superadmin1");
        assertAdmins(permissions, "pipeline2", "superadmin1");

        assertViewers(permissions, "pipeline3", "superadmin1", "group3viewer1", "group3viewer2");
        assertOperators(permissions, "pipeline3", "superadmin1", "group3operator1");
        assertAdmins(permissions, "pipeline3", "superadmin1");

        assertViewers(permissions, "pipeline4", "superadmin1", "group3viewer1", "group3viewer2");
        assertOperators(permissions, "pipeline4", "superadmin1", "group3operator1");
        assertAdmins(permissions, "pipeline4", "superadmin1");
    }

    @Test
    public void shouldAllowAUserToBePartOfDifferentGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline3", "stage1C", "job1C1", "job1C2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "user1", "user2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "user1", "group2");
        configMother.addUserAsOperatorOfPipelineGroup(config, "user1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "user2", "user3"));
        configMother.addRole(config, configMother.createRole("group3_operate_role1", "user3", "user4"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group3_operate_role1", "group3");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2", "pipeline3");

        assertViewers(permissions, "pipeline1", "superadmin1", "user1", "user2");
        assertOperators(permissions, "pipeline1", "superadmin1", "user1", "user2");
        assertAdmins(permissions, "pipeline1", "superadmin1", "user1", "user2");

        assertViewers(permissions, "pipeline2", "superadmin1", "user1");
        assertOperators(permissions, "pipeline2", "superadmin1", "user1");
        assertAdmins(permissions, "pipeline2", "superadmin1");

        assertViewers(permissions, "pipeline3", "superadmin1", "user2", "user3");
        assertOperators(permissions, "pipeline3", "superadmin1", "user3", "user4");
        assertAdmins(permissions, "pipeline3", "superadmin1");
    }

    @Test
    public void shouldConsiderAllUsersAsViewersOperatorsAndAdminsOfAGroupWithNoAuthorizationConfigurationSetup_WhenExplicitSuperAdminsAreNOTSetup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertTrue(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
        assertEveryoneIsAPartOf(pipelinePermissions.operators());
        assertEveryoneIsAPartOf(pipelinePermissions.admins());
    }

    @Test
    public void shouldConsiderAllUsersAsViewersOfAGroupWithNoAuthorizationConfigurationSetup_WhenExplicitSuperAdminsAreSetup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertFalse(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));

        assertThat(pipelinePermissions.viewers().contains("superadmin1"), is(true));
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
    }

    @Test
    public void shouldNotConsiderAllUsersAsOperatorsOfAGroupWithNoAuthorizationConfigurationSetup_WhenExplicitSuperAdminsAreSetup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertFalse(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertOperators(permissions, "pipeline1", "superadmin1");
        assertAdmins(permissions, "pipeline1", "superadmin1");
    }

    @Test
    public void shouldNotConsiderAllUsersAsViewersOfAGroup_WhenExplicitGroupAdminIsSetup() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(not(new Authorization())));

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertViewers(permissions, "pipeline1", "superadmin1", "groupadmin1");
        assertOperators(permissions, "pipeline1", "superadmin1", "groupadmin1");
        assertAdmins(permissions, "pipeline1", "superadmin1", "groupadmin1");
    }

    @Test
    public void shouldAllowStageToOverrideOperators() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addUserAsOperatorOfPipelineGroup(config, "user1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "user2", "group1");

        configMother.addRole(config, configMother.createRole("role1", "user3", "user4"));
        configMother.addRole(config, configMother.createRole("role2", "user5", "user6"));
        configMother.addRoleAsOperatorOfPipelineGroup(config, "role1", "group1");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "role2", "group1");

        StageConfigMother.addApprovalWithUsers(pipelineConfig.first(), "user1");
        StageConfigMother.addApprovalWithRoles(pipelineConfig.first(), "role1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertGroupOperators(permissions, "pipeline1", "superadmin1", "user1", "user2", "user3", "user4", "user5", "user6");
        assertPipelineOperators(permissions, "pipeline1", "superadmin1", "user1", "user3", "user4");
    }

    private Map<CaseInsensitiveString, Permissions> getPipelinesAndTheirPermissions() {
        when(configService.security()).thenReturn(config.server().security());
        when(configService.groups()).thenReturn(config.getGroups());

        return service.pipelinesAndTheirPermissions();
    }

    private void assertViewers(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, String... expectedViewers) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.viewers(), CoreMatchers.<Users>is(new AllowedUsers(s(expectedViewers))));
    }

    private void assertOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, String... expectedOperators) {
        assertGroupOperators(permissionsForAllPipelines, pipelineToCheckFor, expectedOperators);
        assertPipelineOperators(permissionsForAllPipelines, pipelineToCheckFor, expectedOperators);
    }

    private void assertPipelineOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, String... expectedOperators) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.pipelineOperators(), CoreMatchers.<Users>is(new AllowedUsers(s(expectedOperators))));
    }

    private void assertGroupOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, String... expectedOperators) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.operators(), CoreMatchers.<Users>is(new AllowedUsers(s(expectedOperators))));
    }

    private void assertAdmins(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, String... expectedAdmins) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.admins(), CoreMatchers.<Users>is(new AllowedUsers(s(expectedAdmins))));
    }

    private void assertPipelinesInMap(Map<CaseInsensitiveString, Permissions> pipelinesToPermissions, String... expectedPipelines) {
        Set<CaseInsensitiveString> expectedCaseInsensitivePipelineNames = new HashSet<>();
        for (String expectedPipeline : expectedPipelines) {
            expectedCaseInsensitivePipelineNames.add(new CaseInsensitiveString(expectedPipeline));
        }
        assertThat(pipelinesToPermissions.keySet(), is(expectedCaseInsensitivePipelineNames));
    }

    private void assertEveryoneIsAPartOf(Users users) {
        assertThat(users.contains("some-user"), is(true));
        assertThat(users.contains("some-other-user"), is(true));
        assertThat(users.contains("any-random-user"), is(true));
    }
}
