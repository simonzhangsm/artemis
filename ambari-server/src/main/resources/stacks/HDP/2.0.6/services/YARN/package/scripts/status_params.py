#!/usr/bin/env python
"""
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

"""

from resource_management import *


config = Script.get_config()

mapred_user = config['configurations']['global']['mapred_user']
yarn_user = config['configurations']['global']['yarn_user']
yarn_pid_dir_prefix = config['configurations']['global']['yarn_pid_dir_prefix']
mapred_pid_dir_prefix = config['configurations']['global']['mapred_pid_dir_prefix']
yarn_pid_dir = format("{yarn_pid_dir_prefix}/{yarn_user}")
mapred_pid_dir = format("{mapred_pid_dir_prefix}/{mapred_user}")

resourcemanager_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-resourcemanager.pid")
nodemanager_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-nodemanager.pid")
yarn_historyserver_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-historyserver.pid")
mapred_historyserver_pid_file = format("{mapred_pid_dir}/mapred-{mapred_user}-historyserver.pid")
