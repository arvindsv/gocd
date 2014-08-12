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

module Admin
  class SshKeysController < AdminController
    def index
      all_the_keys = ssh_keys_service.all
      all_keys_as_hash_for_json = all_the_keys.collect do |ssh_key| convert_to_hash(ssh_key) end

      render :json => all_keys_as_hash_for_json
    end

    def create
      errors = ssh_keys_service.validate params[:name], params[:hostname], params[:username], params[:key], params[:resources]
      render :json => {:errors => errors.collect {|error| {:key => error.key, :message => error.value}}}, :status => 422 and return unless errors.empty?

      added_key = ssh_keys_service.addKey(params[:name], params[:hostname], params[:username], params[:key], params[:resources])
      render :json => convert_to_hash(added_key)
    end

    def update
      render :json => {:errors => [{:key => 'key_not_found', :message => "Cannot find key with ID: #{params[:id]}"}]}, :status => 422 and return unless ssh_keys_service.hasKey(params[:id])

      errors = ssh_keys_service.validateUpdate(params[:id], params[:name], params[:hostname], params[:username], params[:resources])
      render :json => {:errors => errors.collect {|error| {:key => error.key, :message => error.value}}}, :status => 422 and return unless errors.empty?

      updated_key = ssh_keys_service.updateKey(params[:id], params[:name], params[:hostname], params[:username], params[:resources])
      render :json => convert_to_hash(updated_key)
    end

    def destroy
      render :json => {:errors => [{:key => 'key_not_found', :message => "Cannot find key with ID: #{params[:id]}"}]}, :status => 422 and return unless ssh_keys_service.hasKey(params[:id])

      deleted_key = ssh_keys_service.deleteKey(params[:id])
      render :json => convert_to_hash(deleted_key)
    end

    private

    def convert_to_hash(ssh_key)
      {
          :id => ssh_key.id,
          :name => ssh_key.name,
          :hostname => ssh_key.hostname,
          :username => ssh_key.username,
          :resources => ssh_key.resources
      }
    end
  end
end
