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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe Admin::FeatureTogglesController do
  describe :route do
    it "should resolve route to page for feature toggle listing" do
      {:get => "/admin/feature_toggles"}.should route_to(:controller => "admin/feature_toggles", :action => "index")
      admin_feature_toggles_path.should == "/admin/feature_toggles"
    end
  end

  describe :index do
    it "should serve index template" do
      get :index

      expect(response).to render_template("index")
    end
  end
end