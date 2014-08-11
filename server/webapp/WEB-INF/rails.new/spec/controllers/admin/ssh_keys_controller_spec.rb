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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe Admin::SshKeysController do
  describe :route do
    it "should resolve index" do
      expect({:get => "/admin/ssh_keys"}).to route_to(:controller => "admin/ssh_keys", :action => "index")
    end
  end

  describe :index do
    let (:ssh_key1) { SshKeyMother.key "NAME1", "HOSTNAME1", "USERNAME1", "KEY1", "RESOURCES1" }
    let (:ssh_key2) { SshKeyMother.key "NAME2", "HOSTNAME2", "USERNAME2", "KEY2", "RESOURCES2" }

    before :each do
      ssh_keys_service = stub_service(:ssh_keys_service)
      ssh_keys_service.should_receive(:all) { [ssh_key1, ssh_key2] }

      get :index

      @ssh_keys = JSON.parse(response.body)
    end

    it 'should get a JSON response' do
      expect(response.headers['Content-Type']).to include('application/json')
    end

    it 'should get all the keys' do
      expect(@ssh_keys.size).to eq(2)
    end

    it 'should have all the information' do
      assert_key @ssh_keys[0], "NAME1", "HOSTNAME1", "USERNAME1", "RESOURCES1"
      assert_key @ssh_keys[1], "NAME2", "HOSTNAME2", "USERNAME2", "RESOURCES2"
    end

    it 'should NOT send back the private key info as a part of the response' do
      assert_no_private_key_info_in @ssh_keys[0]
      assert_no_private_key_info_in @ssh_keys[1]
    end
  end

  describe :create do
    before :each do
      @ssh_keys_service = stub_service(:ssh_keys_service)
    end

    it 'should validate ssh key before adding' do
      errors = [ValidationError.new("name", "Is wrong"), ValidationError.new("hostname", "Is also wrong")]
      @ssh_keys_service.should_receive(:validate).with("NAME1", "HOSTNAME1", "USERNAME1", "KEY1", "RESOURCES1") { errors }
      @ssh_keys_service.should_not_receive(:addKey)

      post :create, {:name => "NAME1", :hostname => "HOSTNAME1", :username => "USERNAME1", :key => "KEY1", :resources => "RESOURCES1"}

      output = JSON.parse(response.body)

      expect(response.status).to eq(422)
      expect(output["errors"].size).to eq(2)
      expect(output["errors"][0]).to eq({"key" => "name", "message" => "Is wrong"})
      expect(output["errors"][1]).to eq({"key" => "hostname", "message" => "Is also wrong"})
    end

    it 'should add key if validation passes' do
      new_key = SshKeyMother.key "NAME1", "HOSTNAME1", "USERNAME1", "KEY1", "RESOURCES1"
      @ssh_keys_service.should_receive(:validate).with("NAME1", "HOSTNAME1", "USERNAME1", "KEY1", "RESOURCES1") { [] }
      @ssh_keys_service.should_receive(:addKey).with("NAME1", "HOSTNAME1", "USERNAME1", "KEY1", "RESOURCES1") { new_key }

      post :create, {:name => "NAME1", :hostname => "HOSTNAME1", :username => "USERNAME1", :key => "KEY1", :resources => "RESOURCES1"}

      added_key = JSON.parse(response.body)

      expect(response.status).to eq(200)
      assert_key added_key, "NAME1", "HOSTNAME1", "USERNAME1", "RESOURCES1"
      assert_no_private_key_info_in added_key
    end
  end

  def assert_key key, expected_name, expected_hostname, expected_username, expected_resources
    expect(key["name"]).to eq(expected_name)
    expect(key["hostname"]).to eq(expected_hostname)
    expect(key["username"]).to eq(expected_username)
    expect(key["resources"]).to eq(expected_resources)
  end

  def assert_no_private_key_info_in ssh_key
    expect(ssh_key.keys.sort).to eq(["name", "hostname", "username", "resources"].sort)
  end
end