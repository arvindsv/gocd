##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require 'spec_helper'


describe Admin::JobsController, :ignore_before_filters => true do
  include GoUtil
  describe "actions" do
    render_views

    before do
      controller.stub(:populate_config_validity)
      controller.stub(:checkConfigFileValid)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["job-1", "job-2"].to_java(java.lang.String))
      @artifact1 = ArtifactPlan.new('src', 'dest')
      @artifact2 = ArtifactPlan.new('src2', 'dest2')
      @pipeline.get(0).getJobs().get(0).artifactPlans().add(@artifact1)
      @pipeline.get(0).getJobs().get(0).artifactPlans().add(@artifact2)

      controller.should_receive(:load_pipeline) do
        controller.instance_variable_set('@processed_cruise_config', @cruise_config)
        controller.instance_variable_set('@cruise_config', @cruise_config)
        controller.instance_variable_set('@pipeline', @pipeline)
      end

      allow(controller.pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(PipelinePauseInfo::notPaused)
      allow(controller.system_environment).to receive(:isServerActive).and_return(true)
      allow(controller.version_info_service).to receive(:isGOUpdateCheckEnabled).and_return(false)
      allow(controller.security_service).to receive(:canViewAdminPage).and_return(false)

      allow(controller.flash_message_service).to receive(:get).with('notice').and_return(nil)
    end

    describe "edit settings" do
      it "should display 'Job Name' " do
        get :edit, :stage_parent=> "pipelines", :current_tab => "settings", :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s, :job_name => @pipeline.get(0).getJobs().get(0).name().to_s
        expect(response.status).to eq(200)
        expect(response.body).to have_selector("input[name='job[name]']")
      end
    end

    describe "edit artifacts" do
      include FormUI

      before :each do
        setup_localized_msg_for :key => 'ARTIFACTS', :value => 'Artifacts'
      end

      it "should display artifacts title, instruction and list of artifacts" do
        get :edit,:stage_parent=> "pipelines", :current_tab => "artifacts", :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s, :job_name => @pipeline.get(0).getJobs().get(0).name().to_s

        expect(response.status).to eq(200)
        expect(response.body).to have_selector("h3", :text=>"Artifacts")

        Capybara.string(response.body).find("table[class='artifact']").tap do |table|
          expect(table).to have_selector("input[class='form_input artifact_source'][value='src']")
          expect(table).to have_selector("input[class='form_input artifact_destination'][value='dest']")
          expect(table).to have_selector("input[class='form_input artifact_source'][value='src2']")
          expect(table).to have_selector("input[class='form_input artifact_destination'][value='dest2']")
          table.all("select[class='small']").tap do |select|
            expect(select[0]).to have_selector("option",:text=>"Test Artifact")
            expect(select[0]).to have_selector("option",:text=>"Build Artifact")
          end
        end
      end

      it "should display errors on artifact" do
        error = config_error(ArtifactPlan::SRC, "Source is wrong")
        error.add(ArtifactPlan::DEST, "Dest is wrong")
        set(@artifact1, "errors", error)

        get :edit, :stage_parent=> "pipelines", :current_tab => :artifacts, :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s, :job_name => @pipeline.get(0).getJobs().get(0).name().to_s

        expect(response.status).to eq(200)
        expect(response.body).to have_selector("h3", :text=>"Artifacts")
        Capybara.string(response.body).find("table[class='artifact']").tap do |table|
          expect(table).to have_selector("div.field_with_errors input[class='form_input artifact_source'][value='src']")
          expect(table).to have_selector("div.form_error", :text=>"Source is wrong")
          expect(table).to have_selector("div.field_with_errors input[class='form_input artifact_destination'][value='dest']")
          expect(table).to have_selector("div.form_error", :text=>"Dest is wrong")
        end
      end
    end
  end
end
