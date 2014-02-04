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

App.Dataset = DS.Model.extend({
  name: DS.attr('string'),
  sourceClusterName: DS.attr('string'),
  targetClusterName: DS.attr('string'),
  sourceDir: DS.attr('string'),
  targetDir: DS.attr('string'),
  frequency: DS.attr('string'),
  frequencyUnit: DS.attr('string'),
  scheduleStartDate: DS.attr('string'),
  scheduleEndDate: DS.attr('string'),
  datasetJobs: DS.hasMany('App.DataSetJob'),

  status: function () {
    var jobs = this.get('datasetJobs').toArray();
    if (jobs.someProperty('status', 'RUNNING')) {
      return 'RUNNING';
    } else if (jobs.someProperty('status', 'SUSPENDED')) {
      return 'SUSPENDED';
    } else {
      return 'SCHEDULED';
    }
  }.property('datasetJobs', 'datasetJobs.@each.status'),

  statusFormatted: function (){
    return this.get('status').toLowerCase().capitalize();
  }.property('status'),

  isRunning: function () {
    return this.get('status') === 'RUNNING';
  }.property('status'),

  isSuspended: function () {
    return this.get('status') === 'SUSPENDED';
  }.property('status'),

  isScheduled: function () {
    return this.get('status') === 'SCHEDULED';
  }.property('status'),

  //Last succeeded date. Will be calculated later.
  lastSucceededDate: function () {
    return '';
  }.property(),

  //Next instance to run. Will be calculated later.
  nextInstance: function () {
    return '';
  }.property(),

  //Class name for dataset health status indicator
  healthClass: function () {
    var jobs = this.get('datasetJobs').toArray();
    jobs = jobs.filterProperty('status', 'FAILED').concat(jobs.filterProperty('status', 'SUCCESSFUL'));
    jobs.sort(function (a, b) {
      return a.get('endDate') - b.get('endDate');
    });
    return jobs.length && jobs[0].get('status') === 'FAILED' ? 'health-status-DEAD-RED' : 'health-status-LIVE';
  }.property('datasetJobs', 'datasetJobs.@each.status')
});

App.Dataset.FIXTURES = [];