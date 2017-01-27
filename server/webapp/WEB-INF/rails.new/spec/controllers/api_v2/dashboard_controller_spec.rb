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

require 'spec_helper'

describe ApiV2::DashboardController do
  include GoDashboardPipelineMother

  before do
    @user = Username.new(CaseInsensitiveString.new("foo"))

    controller.stub(:current_user).and_return(@user)
    controller.stub(:populate_config_validity)

    @go_dashboard_service = stub_service(:go_dashboard_service)
    @pipeline_selections_service = stub_service(:pipeline_selections_service)
  end

  describe :dashboard do

    before(:each) do
      clock = double('Clock')
      clock.stub(:currentTimeMillis).and_return(11111)
      @timestamp_provider = com.thoughtworks.go.server.dashboard.TimeStampBasedCounter.new(clock)
    end

    it 'should accept only number for If-Go-Dashboard-Modified-Since header' do
      controller.request.env['HTTP_IF_GO_DASHBOARD_MODIFIED_SINCE'] = "Garbage"
      get_with_api_header :dashboard

      expect(response.status).to eq(412)
      expect(actual_response).to eq({message: "Please provide a numeric value for header 'If-Go-Dashboard-Modified-Since'"})
    end

    it 'should get dashboard json' do
      all_pipelines = [dashboard_pipeline("pipeline1"), dashboard_pipeline("pipeline2")]
      go_dashboard_pipelines = GoDashboardPipelines.new(all_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)
      @pipeline_selections_service.should_receive(:getSelectedPipelines).with(anything, anything).and_return(PipelineSelections::ALL)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(all_pipelines, ApiV2::Dashboard::PipelineGroupsRepresenter))
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end

    it 'should return only modified pipelines based on If-Go-Dashboard-Modified-Since' do
      pipeline1 = dashboard_pipeline("pipeline1", "group1", Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), 3000)
      pipeline2 = dashboard_pipeline("pipeline2", "group1", Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), 1000)
      all_pipelines = [pipeline1, pipeline2]
      go_dashboard_pipelines = GoDashboardPipelines.new(all_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)
      @pipeline_selections_service.should_receive(:getSelectedPipelines).with(anything, anything).and_return(PipelineSelections::ALL)

      controller.request.env['HTTP_IF_GO_DASHBOARD_MODIFIED_SINCE'] = "2000"
      get_with_api_header :dashboard
      expect(response.status).to be(206)
      expect(actual_response).to eq(expected_response([pipeline1], ApiV2::Dashboard::PipelineGroupsRepresenter))
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end

    it 'should get empty json when dashboard is empty' do
      no_pipelines = []
      go_dashboard_pipelines = GoDashboardPipelines.new(no_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)
      @pipeline_selections_service.should_receive(:getSelectedPipelines).with(anything, anything).and_return(PipelineSelections::ALL)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(no_pipelines, ApiV2::Dashboard::PipelineGroupsRepresenter))
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end

    it 'should not output any pipelines which the current user does not have permission to view' do
      permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)

      pipeline_which_user_can_see = dashboard_pipeline("pipeline1", "group1")
      pipeline_which_user_cannot_see = dashboard_pipeline("pipeline2", "group1", permissions)

      all_pipelines = [pipeline_which_user_can_see, pipeline_which_user_cannot_see]
      expected_pipelines_in_output = [pipeline_which_user_can_see]
      go_dashboard_pipelines = GoDashboardPipelines.new(all_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)
      @pipeline_selections_service.should_receive(:getSelectedPipelines).with(anything, anything).and_return(PipelineSelections::ALL)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(expected_pipelines_in_output, ApiV2::Dashboard::PipelineGroupsRepresenter))
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end

    it "should not output any pipelines which are not included in user's pipeline selections" do
      permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)

      pipeline_which_user_can_see = dashboard_pipeline("pipeline1", "group1")
      pipeline_which_user_cannot_see = dashboard_pipeline("pipeline2", "group1", permissions)
      pipeline_which_user_has_permission_to_see_by_filtered = dashboard_pipeline("pipeline3", "group1")

      all_pipelines = [pipeline_which_user_can_see, pipeline_which_user_cannot_see, pipeline_which_user_has_permission_to_see_by_filtered]
      expected_pipelines_in_output = [pipeline_which_user_can_see]
      go_dashboard_pipelines = GoDashboardPipelines.new(all_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)
      @pipeline_selections_service.should_receive(:getSelectedPipelines).with(anything, anything).and_return(PipelineSelections.new(Arrays::asList("pipeline3")))

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(expected_pipelines_in_output, ApiV2::Dashboard::PipelineGroupsRepresenter))
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end

    it "should not output any pipelines if no new changes have happened since Go-Dashboard-Last-Modified" do
      pipeline1 = dashboard_pipeline("pipeline1", "group1", Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), 3000)
      pipeline2 = dashboard_pipeline("pipeline2", "group1", Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), 1000)
      all_pipelines = [pipeline1, pipeline2]
      go_dashboard_pipelines = GoDashboardPipelines.new(all_pipelines, @timestamp_provider)
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(go_dashboard_pipelines)

      controller.request.env['HTTP_IF_GO_DASHBOARD_MODIFIED_SINCE'] = go_dashboard_pipelines.lastUpdatedTimeStamp()
      get_with_api_header :dashboard
      expect(response.status).to eq(304)
      expect(actual_response).to eq({})
      expect(response.headers['Go-Dashboard-Last-Modified']).to eq(go_dashboard_pipelines.lastUpdatedTimeStamp().to_s)
    end
  end
end
