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
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/* Understands which users can view, operate and administer which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private GoConfigService goConfigService;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions() {
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = new HashMap<>();

        final SecurityConfig security = goConfigService.security();
        final Map<String, Collection<String>> rolesToUsers = rolesToUsers(security);
        final Set<String> superAdmins = namesOf(security.adminsConfig(), rolesToUsers);

        goConfigService.groups().accept(new PipelineGroupVisitor() {
            @Override
            public void visit(PipelineConfigs group) {
                Set<String> viewers = new HashSet<>();
                Set<String> operators = new HashSet<>();
                Set<String> admins = new HashSet<>();

                Set<String> pipelineGroupViewers = namesOf(group.getAuthorization().getViewConfig(), rolesToUsers);
                Set<String> pipelineGroupOperators = namesOf(group.getAuthorization().getOperationConfig(), rolesToUsers);
                Set<String> pipelineGroupAdmins = namesOf(group.getAuthorization().getAdminsConfig(), rolesToUsers);

                admins.addAll(superAdmins);
                admins.addAll(pipelineGroupAdmins);

                operators.addAll(admins);
                operators.addAll(pipelineGroupOperators);

                viewers.addAll(admins);
                viewers.addAll(pipelineGroupViewers);

                boolean hasNoAdminsDefinedAtRootLevel = security.adminsConfig().isEmpty();
                boolean hasNoAuthDefinedAtGroupLevel = !group.hasAuthorizationDefined();

                for (PipelineConfig pipeline : group) {
                    if (hasNoAdminsDefinedAtRootLevel) {
                        pipelinesAndTheirPermissions.put(pipeline.name(), new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE));
                    } else if (hasNoAuthDefinedAtGroupLevel) {
                        pipelinesAndTheirPermissions.put(pipeline.name(), new Permissions(Everyone.INSTANCE, new AllowedUsers(admins), new AllowedUsers(admins)));
                    } else {
                        pipelinesAndTheirPermissions.put(pipeline.name(), new Permissions(new AllowedUsers(viewers), new AllowedUsers(operators), new AllowedUsers(admins)));
                    }
                }
            }
        });

        return pipelinesAndTheirPermissions;
    }

    private Set<String> namesOf(AdminsConfig adminsConfig, Map<String, Collection<String>> rolesToUsers) {
        List<AdminUser> superAdmins = adminsConfig.getUsers();
        Set<String> superAdminNames = new HashSet<>();

        for (AdminUser superAdminUser : superAdmins) {
            superAdminNames.add(superAdminUser.getName().toString());
        }

        for (AdminRole superAdminRole : adminsConfig.getRoles()) {
            superAdminNames.addAll(rolesToUsers.get(superAdminRole.getName().toString()));
        }

        return superAdminNames;
    }

    private Map<String, Collection<String>> rolesToUsers(SecurityConfig securityConfig) {
        Map<String, Collection<String>> rolesToUsers = new HashMap<>();
        for (Role role : securityConfig.getRoles()) {
            rolesToUsers.put(role.getName().toString(), role.usersOfRole());
        }
        return rolesToUsers;
    }
}
