/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

var FeatureToggles = (function() {
    var FeatureTogglesView = function(page_container, container, template_element) {
        var addAllToggles = function(data) {
            var template = Handlebars.compile(template_element.html());
            var replacedText = template({toggles: data});
            container.html(replacedText);
        };

        var showMessage = function(message) {
            page_container.find("#global-message").find(".message-text").text(message);
        };

        return {
            addAllToggles: addAllToggles,
            showMessage: showMessage
        }
    };

    var FeatureTogglesController = function(view, api_path_prefix) {
        var getMessageFrom = function(error_response) {
            try {
                return JSON.parse(error_response).message;
            } catch (e) {
                return error_response;
            }
        };

        var allToggles = function() {
            jQuery.getJSON(api_path_prefix)
                .done(function (data) {
                    view.addAllToggles(data)
                })
                .fail(function (jQXHR) {
                    view.showMessage(getMessageFrom(jQXHR.responseText));
                })
        };

        return {
            listAllToggles: allToggles
        }
    };

    var initializeModule = function(dom_container_of_page, dom_container_of_toggles, dom_toggle_template, api_path_prefix) {
        var view = FeatureTogglesView(dom_container_of_page, dom_container_of_toggles, dom_toggle_template);
        var controller = FeatureTogglesController(view, api_path_prefix);

        controller.listAllToggles();
    };

    return {
        initializeAt: initializeModule
    };
})();