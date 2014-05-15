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

Ambari Agent

"""

import os
import sys

from resource_management import *


def yarn(name=None):
  import params


  if name in ["nodemanager", "historyserver"]:
    if params.yarn_log_aggregation_enabled:
      params.HdfsDirectory(params.yarn_nm_app_log_dir,
                           action="create_delayed",
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0o777,
                           recursive_chmod=True
      )
    params.HdfsDirectory("/mapred",
                         action="create_delayed",
                         owner=params.mapred_user
    )
    params.HdfsDirectory("/mapred/system",
                         action="create_delayed",
                         owner=params.hdfs_user
    )
    params.HdfsDirectory(params.mapreduce_jobhistory_intermediate_done_dir,
                         action="create_delayed",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0o777
    )

    params.HdfsDirectory(params.mapreduce_jobhistory_done_dir,
                         action="create_delayed",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0o1777
    )
    params.HdfsDirectory(None, action="create")

  Directory([params.yarn_pid_dir, params.yarn_log_dir],
            owner=params.yarn_user,
            group=params.user_group,
            recursive=True
  )

  Directory([params.mapred_pid_dir, params.mapred_log_dir],
            owner=params.mapred_user,
            group=params.user_group,
            recursive=True
  )
  Directory(params.nm_local_dirs.split(',') + params.nm_log_dirs.split(',') + [params.yarn_log_dir_prefix],
            owner=params.yarn_user,
            recursive=True,
            ignore_failures=True,
  )

  XmlConfig("core-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0o644
  )

  XmlConfig("mapred-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['mapred-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0o644
  )

  XmlConfig("yarn-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0o644
  )

  XmlConfig("capacity-scheduler.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0o644
  )

  if name == 'resourcemanager':
    File(params.yarn_job_summary_log,
       owner=params.yarn_user,
       group=params.user_group
    )

  File(params.rm_nodes_exclude_path,
       owner=params.yarn_user,
       group=params.user_group
  )

  File(format("{limits_conf_dir}/yarn.conf"),
       mode=0o644,
       content=Template('yarn.conf.j2')
  )

  File(format("{limits_conf_dir}/mapreduce.conf"),
       mode=0o644,
       content=Template('mapreduce.conf.j2')
  )

  File(format("{config_dir}/yarn-env.sh"),
       owner=params.yarn_user,
       group=params.user_group,
       mode=0o755,
       content=Template('yarn-env.sh.j2')
  )

  if params.security_enabled:
    container_executor = format("{yarn_container_bin}/container-executor")
    File(container_executor,
         group=params.yarn_executor_container_group,
         mode=0o6050
    )
    
    File(format("{config_dir}/container-executor.cfg"),
         group=params.user_group,
         mode=0o644,
         content=Template('container-executor.cfg.j2')
    )


  if params.security_enabled:
    tc_mode = 0o644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user
  File(format("{config_dir}/mapred-env.sh"),
       owner=tc_owner,
       content=Template('mapred-env.sh.j2')
  )
  if params.security_enabled:
    File(os.path.join(params.hadoop_bin, "task-controller"),
         owner="root",
         group=params.mapred_tt_group,
         mode=0o6050
    )
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner=tc_owner,
         mode=tc_mode,
         group=params.mapred_tt_group,
         content=Template("taskcontroller.cfg.j2")
    )
  else:
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner=tc_owner,
         content=Template("taskcontroller.cfg.j2")
    )
  if "mapred-site" in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['mapred-site'],
              owner=params.mapred_user,
              group=params.user_group
    )
  if "mapred-queue-acls" in params.config['configurations']:
    XmlConfig("mapred-queue-acls.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'mapred-queue-acls'],
              owner=params.mapred_user,
              group=params.user_group
    )
  elif os.path.exists(
    os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml")):
    File(os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml"),
         owner=params.mapred_user,
         group=params.user_group
    )
  if "capacity-scheduler" in params.config['configurations']:
    XmlConfig("capacity-scheduler.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'capacity-scheduler'],
              owner=params.hdfs_user,
              group=params.user_group
    )
  if os.path.exists(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml')):
    File(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml'),
         owner=params.mapred_user,
         group=params.user_group
    )
  if os.path.exists(
    os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )
  if os.path.exists(
    os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )
