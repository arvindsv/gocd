/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.dashboard;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonOutputWriter;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class DashboardControllerDelegate extends ApiController {

    private final PipelineSelectionsService pipelineSelectionsService;
    private final GoDashboardService goDashboardService;

    public DashboardControllerDelegate(PipelineSelectionsService pipelineSelectionsService, GoDashboardService goDashboardService) {
        super(ApiVersion.v2);
        this.pipelineSelectionsService = pipelineSelectionsService;
        this.goDashboardService = goDashboardService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Dashboard.SELF;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("", this::verifyContentType);
            get("", this::index, GsonTransformer.getInstance());
        });
    }

    private Logger logger = LoggerFactory.getLogger("DashboardControllerDelegate");

    private Map index(Request request, Response response) throws IOException {
        String selectedPipelinesCookie = request.cookie("selected_pipelines");
        Long userId = currentUserId(request);
        Username userName = currentUsername();

        long start = System.currentTimeMillis();
        PipelineSelections selectedPipelines = pipelineSelectionsService.getPersistedSelectedPipelines(selectedPipelinesCookie, userId);

        long part1 = System.currentTimeMillis();
        List<GoDashboardPipelineGroup> pipelineGroups = goDashboardService.allPipelineGroupsForDashboard(selectedPipelines, userName);

        long part2 = System.currentTimeMillis();
        if ("true".equals(request.queryParams("stream"))) {
            response.header("streaming", "true");
            Writer writer = response.raw().getWriter();
            if ("true".equals(request.queryParams("buffer"))) {
                response.header("buffering", "true");
                writer = new BufferedWriter(writer);
            }

            try {
                new JsonOutputWriter(writer, RequestContext.requestContext(request)).forTopLevelObject(jsonOutputWriter -> {
                    PipelineGroupsRepresenter.newToJSON(jsonOutputWriter, pipelineGroups, userName);
                });
            } finally {
                writer.flush();
            }

            long end = System.currentTimeMillis();

            logger.error("GET_SELECTED_PIPELINES: {}, LOAD_PIPELINE_GROUPS: {}, WRITE_TO_WRITER: {}",
                    part1 - start, part2 - part1, end - part2);
            return null;
        }

        Map toJSON = PipelineGroupsRepresenter.toJSON(pipelineGroups, RequestContext.requestContext(request), userName);

        long end = System.currentTimeMillis();
        logger.error("GET_SELECTED_PIPELINES: {}, LOAD_PIPELINE_GROUPS: {}, TO_JSON_MAP: {}",
                part1 - start, part2 - part1, end - part2);

        return toJSON;
    }
}
