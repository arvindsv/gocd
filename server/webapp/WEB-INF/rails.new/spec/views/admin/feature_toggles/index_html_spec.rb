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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/feature_toggles/index.html.erb" do
  it "should have the javascript to initialize the page" do
    render

    expect(response.body).to include('FeatureToggles.initializeAt(jQuery("#feature_toggles_content"), jQuery("#feature_toggles"), jQuery("#feature-toggle-item-template"), "/api/admin/feature_toggles");')
  end

  it "should have a template to be used for feature toggles" do
    render

    expect(response.body).to have_selector("script#feature-toggle-item-template", :visible => false)

    template_content = Capybara.string(response.body).find("script#feature-toggle-item-template", :visible => false).text
    expect(template_content).to have_selector("ul li span.key")
    expect(template_content).to have_selector("ul li span.description")
    expect(template_content).to have_selector("ul li span.value")
    expect(template_content).to have_selector("ul li span.has_changed")
  end
end