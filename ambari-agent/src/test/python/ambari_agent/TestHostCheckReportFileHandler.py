#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

from unittest import TestCase
import unittest
import os
import tempfile
from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler
import logging
import configparser

class TestHostCheckReportFileHandler(TestCase):

  logger = logging.getLogger()

  def test_write_host_check_report_really_empty(self):
    tmpfile = tempfile.mktemp()

    config = configparser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)
    dict = {}
    handler.writeHostCheckFile(dict)

    configValidator = configparser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    if configValidator.has_section('users'):
      users = configValidator.get('users', 'usr_list')
      self.assertEqual(users, '')

  def test_write_host_check_report_empty(self):
    tmpfile = tempfile.mktemp()

    config = configparser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)
    dict = {}
    dict['hostHealth'] = {}
    dict['existingUsers'] = []
    dict['alternatives'] = []
    dict['stackFoldersAndFiles'] = []
    dict['hostHealth']['activeJavaProcs'] = []
    dict['installedPackages'] = []
    dict['existingRepos'] = []

    handler.writeHostCheckFile(dict)

    configValidator = configparser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    users = configValidator.get('users', 'usr_list')
    users = configValidator.get('users', 'usr_homedir_list')
    self.assertEqual(users, '')
    names = configValidator.get('alternatives', 'symlink_list')
    targets = configValidator.get('alternatives', 'target_list')
    self.assertEqual(names, '')
    self.assertEqual(targets, '')

    paths = configValidator.get('directories', 'dir_list')
    self.assertEqual(paths, '')

    procs = configValidator.get('processes', 'proc_list')
    self.assertEqual(procs, '')

    pkgs = configValidator.get('packages', 'pkg_list')
    self.assertEqual(pkgs, '')

    repos = configValidator.get('repositories', 'repo_list')
    self.assertEqual(repos, '')

    time = configValidator.get('metadata', 'created')
    self.assertTrue(time != None)

  def test_write_host_check_report(self):
    tmpfile = tempfile.mktemp()

    config = configparser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)

    dict = {}
    dict['hostHealth'] = {}
    dict['existingUsers'] = [{'name':'user1', 'homeDir':'/var/log', 'status':'Exists'}]
    dict['alternatives'] = [
      {'name':'/etc/alternatives/hadoop-conf', 'target':'/etc/hadoop/conf.dist'},
      {'name':'/etc/alternatives/hbase-conf', 'target':'/etc/hbase/conf.1'}
    ]
    dict['stackFoldersAndFiles'] = [{'name':'/a/b', 'type':'directory'}, {'name':'/a/b.txt', 'type':'file'}]
    dict['hostHealth']['activeJavaProcs'] = [
      {'pid':355, 'hadoop':True, 'command':'some command', 'user':'root'},
      {'pid':455, 'hadoop':True, 'command':'some command', 'user':'hdfs'}
    ]
    dict['installedPackages'] = [
      {'name':'hadoop', 'version':'3.2.3', 'repoName':'HDP'},
      {'name':'hadoop-lib', 'version':'3.2.3', 'repoName':'HDP'}
    ]
    dict['existingRepos'] = ['HDP', 'HDP-epel']
    handler.writeHostCheckFile(dict)

    configValidator = configparser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    users = configValidator.get('users', 'usr_list')
    homedirs = configValidator.get('users', 'usr_homedir_list')
    self.assertEqual(users, 'user1')
    self.assertEqual(homedirs, '/var/log')

    names = configValidator.get('alternatives', 'symlink_list')
    targets = configValidator.get('alternatives', 'target_list')
    self.chkItemsEqual(names, ['/etc/alternatives/hadoop-conf', '/etc/alternatives/hbase-conf'])
    self.chkItemsEqual(targets, ['/etc/hadoop/conf.dist', '/etc/hbase/conf.1'])

    paths = configValidator.get('directories', 'dir_list')
    self.chkItemsEqual(paths, ['/a/b', '/a/b.txt'])

    procs = configValidator.get('processes', 'proc_list')
    self.chkItemsEqual(procs, ['455', '355'])

    pkgs = configValidator.get('packages', 'pkg_list')
    self.chkItemsEqual(pkgs, ['hadoop', 'hadoop-lib'])

    repos = configValidator.get('repositories', 'repo_list')
    self.chkItemsEqual(repos, ['HDP', 'HDP-epel'])

    time = configValidator.get('metadata', 'created')
    self.assertTrue(time != None)

  def chkItemsEqual(self, commaDelimited, items):
    items1 = commaDelimited.split(',')
    items1.sort()
    items.sort()
    items1Str = ','.join(items1)
    items2Str = ','.join(items)
    self.assertEqual(items1Str, items2Str)

if __name__ == "__main__":
  unittest.main(verbosity=2)
