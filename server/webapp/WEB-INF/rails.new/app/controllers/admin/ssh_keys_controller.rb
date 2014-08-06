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

      all_keys_as_hash_for_json = all_the_keys.collect do |ssh_key|
        {
            :name => ssh_key.name,
            :hostname => ssh_key.hostname,
            :username => ssh_key.username,
            :resources => ssh_key.resources
        }
      end

      render :json => all_keys_as_hash_for_json
    end
  end
end
