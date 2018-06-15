/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv6.shared.representers.stages.tasks

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv6.shared.representers.stages.ConfigHelperOptions
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.ExecTask
import com.thoughtworks.go.config.materials.PasswordDeserializer
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock

class ExecTaskRepresenterTest implements TaskRepresenterTest {
  def existingTask() {
    def task = new ExecTask()
    task.setCommand("sleep")
    task.setArgsList("300")
    task.setWorkingDirectory("f/m/l")
    return task
  }

  def defaultNewTask() {
    return new ExecTask()
  }

  def expectedTaskHash =
    [
      type      : 'exec',
      attributes: [
        working_directory: 'f/m/l',
        command          : 'sleep',
        arguments        : ['300'],
        run_if           : []
      ]
    ]

  def expectedTaskHashWithRunIf =
    [
      type      : 'exec',
      attributes: [
        working_directory: 'f/m/l',
        command          : 'sleep',
        arguments        : ['300'],
        run_if           : ['passed', 'failed', 'any']
      ]
    ]

  def expectedTaskHashWithNoAttributes =
    [
      type      : 'exec'
    ]

  def expectedTaskHashWithOnCancelConfig =
    [
      type      : 'exec',
      attributes: [
        working_directory: 'f/m/l',
        command          : 'sleep',
        arguments        : ['300'],
        run_if           : [],
        on_cancel      : ["type": "ant", attributes:[
          run_if: [],
          working_directory: null,
          build_file: null,
          target: null
        ]]
      ]
    ]

  @Test
  void 'should convert json with no attributes to Task'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedTaskHashWithNoAttributes)
    def task = TaskRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), mock(PasswordDeserializer.class)))

    def expectedTask = defaultNewTask()
    assertThatJson(task).isEqualTo(expectedTask)
  }
}
