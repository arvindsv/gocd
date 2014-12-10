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

describe("feature_toggles", function () {
    beforeEach(function () {
        setFixtures("<div id=\"feature_toggles_content\">\n" +
        "    <h3>Feature Toggles</h3>\n" +
        "\n" +
        "    <div id=\"global-message\">Failed to fetch toggles: <span class='message-text'></span></div>\n" +
        "\n" +
        "    <script id=\"feature-toggle-item-template\" type=\"text/x-handlebars-template\">\n" +
        "        <ul>\n" +
        "            {{#each toggles}}\n" +
        "                <li id=\"{{this.key}}\">\n" +
        "                    <span class=\"key\">{{this.key}}</span>\n" +
        "                    <span class=\"description\">{{this.description}}</span>\n" +
        "                    <span class=\"value\">{{this.value}}</span>\n" +
        "                    <span class=\"has_changed\">{{this.has_changed}}</span>\n" +
        "                    <span class=\"message\"></span>\n" +
        "                </li>\n" +
        "            {{/each}}\n" +
        "        </ul>\n" +
        "    </script>\n" +
        "\n" +
        "    <div id=\"feature_toggles\">\n" +
        "    </div>\n" +
        "</div>\n");
    });

    beforeEach(function() {
        jasmine.Ajax.install();
    });

    afterEach(function() {
        jasmine.Ajax.uninstall();
    });

    it("should list all toggles when list API call succeeds", function () {
        var api_path = "/specified/api/url";
        jasmine.Ajax.stubRequest(api_path).andReturn({"responseText": JSON.stringify(
            [
                {key: 'key1', description: 'desc1', value: true, has_changed: true},
                {key: 'key2', description: 'desc2', value: false, has_changed: false}
        ])});


        FeatureToggles.initializeAt(jQuery("#feature_toggles_content"), jQuery("#feature_toggles"), jQuery("#feature-toggle-item-template"), api_path);


        expect(jQuery("#feature_toggles_content").find("#feature_toggles ul li").length).toEqual(2);

        var first_toggle_item = jQuery(jQuery("#feature_toggles_content").find("#feature_toggles ul li")[0]);
        expect(first_toggle_item.attr("id")).toEqual("key1");
        expect(first_toggle_item.find(".key").text()).toEqual("key1");
        expect(first_toggle_item.find(".description").text()).toEqual("desc1");
        expect(first_toggle_item.find(".value").text()).toEqual("true");
        expect(first_toggle_item.find(".has_changed").text()).toEqual("true");

        var second_toggle_item = jQuery(jQuery("#feature_toggles_content").find("#feature_toggles ul li")[1]);
        expect(second_toggle_item.attr("id")).toEqual("key2");
        expect(second_toggle_item.find(".key").text()).toEqual("key2");
        expect(second_toggle_item.find(".description").text()).toEqual("desc2");
        expect(second_toggle_item.find(".value").text()).toEqual("false");
        expect(second_toggle_item.find(".has_changed").text()).toEqual("false");
    });

    it("should list NO toggles when list API call fails", function () {
        var api_path = "/specified/api/url";
        jasmine.Ajax.stubRequest(api_path).andReturn({
            "status": 422,
            "responseText": JSON.stringify({"message": "Something went wrong"})});

        FeatureToggles.initializeAt(jQuery("#feature_toggles_content"), jQuery("#feature_toggles"), jQuery("#feature-toggle-item-template"), api_path);


        expect(jQuery("#feature_toggles_content").find("#feature_toggles ul li").length).toEqual(0);
    });

    it("should show message when list API call fails with proper JSON failure response", function () {
        var api_path = "/specified/api/url";
        jasmine.Ajax.stubRequest(api_path).andReturn({
            "status": 422,
            "responseText": JSON.stringify({"message": "Something went wrong"})});

        FeatureToggles.initializeAt(jQuery("#feature_toggles_content"), jQuery("#feature_toggles"), jQuery("#feature-toggle-item-template"), api_path);


        expect(jQuery("#feature_toggles_content").find("#global-message .message-text").text()).toEqual("Something went wrong");
    });

    it("should show received error message directly, when list API call fails with a non-JSON response", function () {
        var api_path = "/specified/api/url";
        jasmine.Ajax.stubRequest(api_path).andReturn({
            "status": 500,
            "responseText": "<b>Some non JSON response ...</b>"});

        FeatureToggles.initializeAt(jQuery("#feature_toggles_content"), jQuery("#feature_toggles"), jQuery("#feature-toggle-item-template"), api_path);


        expect(jQuery("#feature_toggles_content").find("#global-message .message-text").text()).toEqual("<b>Some non JSON response ...</b>");
    })
});
