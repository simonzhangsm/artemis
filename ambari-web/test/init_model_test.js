/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
require('models/stack_service_component');
require('mappers/server_data_mapper');
require('mappers/stack_service_component_mapper');

module.exports = {
  setupStackServiceComponent: function() {
    /**
     * initialization of App.StackServiceComponent model
     * @type {*}
     */
    var data = {items: Em.A([])};
    require('test/service_components').items.forEach(function(i) {
      i.serviceComponents.forEach(function(sc) {
        data.items.pushObject(sc.StackServiceComponents);
      });
    });
    App.stackServiceComponentMapper.map(data);
  },
  cleanStackServiceComponent: function(){
    App.StackServiceComponent.find().set('content',[]);
  },
  setupStackVersion: function(context, version) {
    context.prevStackVersion = App.get('currentStackVersion');
    App.set('currentStackVersion', version);
  },
  restoreStackVersion: function(context) {
    App.set('currentStackVersion', context.prevStackVersion);
  },
  configs: require('test/mock_data_setup/configs_mock_data')
};