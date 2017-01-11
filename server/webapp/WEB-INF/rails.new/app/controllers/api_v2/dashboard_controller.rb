##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################
module ApiV2
  class DashboardController < ApiV2::BaseController

    include ApplicationHelper

    before_filter :check_timestamp

    def dashboard
      name_of_current_user = CaseInsensitiveString.str(current_user.getUsername())

      pipeline_selections = pipeline_selections_service.getSelectedPipelines(cookies[:selected_pipelines], current_user_entity_id)

      all_pipelines_for_dashboard = go_dashboard_service.allPipelinesForDashboard()
      pipelines_across_groups = all_pipelines_for_dashboard.orderedEntries()

      dashboard_last_updated_time_stamp_header = request.headers["Go-Dashboard-Last-Updated-Timestamp"].to_i || 0
      pipelines_viewable_by_user = pipelines_across_groups.select do |pipeline|
        has_pipeline_been_updated_since_last_fetch = pipeline.lastUpdatedTimeStamp() > dashboard_last_updated_time_stamp_header
        does_user_have_view_permissions = pipeline.canBeViewedBy(name_of_current_user)
        has_user_selected_the_pipeline_for_display = pipeline_selections.includesPipeline(pipeline.name())

        does_user_have_view_permissions && has_user_selected_the_pipeline_for_display && has_pipeline_been_updated_since_last_fetch
      end

      presenters = Dashboard::PipelineGroupsRepresenter.new(pipelines_viewable_by_user)
      response.headers['Go-Dashboard-Last-Updated-Timestamp'] = all_pipelines_for_dashboard.lastUpdatedTimeStamp().to_s
      render DEFAULT_FORMAT => presenters.to_hash(url_builder: self)
    end

    private
    def check_timestamp
      render_message("Please provide a numeric value for header 'Go-Dashboard-Last-Updated-Timestamp'", :precondition_failed)  if !request.headers["Go-Dashboard-Last-Updated-Timestamp"].blank? && request.headers["Go-Dashboard-Last-Updated-Timestamp"].match("^[0-9]+$").nil?
    end
  end
end
