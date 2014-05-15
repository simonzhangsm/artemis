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

import datetime

from mock import MagicMock, call, patch

from stacks.utils.RMFTestCase import *
import  resource_management.libraries.functions


@patch.object(resource_management.libraries.functions, "get_unique_id_and_date", new=MagicMock(return_value=''))

class TestStormServiceCheck(RMFTestCase):

  def test_service_check(self):
    self.executeScript("2.1/services/STORM/package/scripts/service_check.py",
                       classname="ServiceCheck",
                       command="service_check",
                       config_file="default.json"
    )

    self.assertResourceCalled('File', '/tmp/wordCount.jar',
      content=StaticFile('wordCount.jar'),
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 storm jar /tmp/wordCount.jar storm.starter.WordCountTopology WordCount -c nimbus.host=c6402.ambari.apache.org',
      logoutput=True,
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 storm kill WordCount')

