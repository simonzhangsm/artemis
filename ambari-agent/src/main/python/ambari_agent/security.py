#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import http.client
import urllib.request, urllib.error, urllib.parse
import socket
import ssl
import os
import logging
import subprocess
import json
import pprint
import traceback
import hostname

logger = logging.getLogger()

GEN_AGENT_KEY = "openssl req -new -newkey rsa:1024 -nodes -keyout %(keysdir)s/%(hostname)s.key\
	-subj /OU=%(hostname)s/\
        -out %(keysdir)s/%(hostname)s.csr"


class VerifiedHTTPSConnection(http.client.HTTPSConnection):
  """ Connecting using ssl wrapped sockets """
  def __init__(self, host, port=None, config=None):
    http.client.HTTPSConnection.__init__(self, host, port=port)
    self.config = config
    self.two_way_ssl_required = False

  def connect(self):

    if not self.two_way_ssl_required:
      try:
        sock = self.create_connection()
        self.sock = ssl.wrap_socket(sock, cert_reqs=ssl.CERT_NONE)
        logger.info('SSL connection established. Two-way SSL authentication is '
                    'turned off on the server.')
      except (ssl.SSLError, AttributeError):
        self.two_way_ssl_required = True
        logger.info('Insecure connection to https://' + self.host + ':' + self.port + 
                    '/ failed. Reconnecting using two-way SSL authentication..')

    if self.two_way_ssl_required:
      self.certMan = CertificateManager(self.config)
      self.certMan.initSecurity()
      agent_key = self.certMan.getAgentKeyName()
      agent_crt = self.certMan.getAgentCrtName()
      server_crt = self.certMan.getSrvrCrtName()

      sock = self.create_connection()

      try:
        self.sock = ssl.wrap_socket(sock,
                                keyfile=agent_key,
                                certfile=agent_crt,
                                cert_reqs=ssl.CERT_REQUIRED,
                                ca_certs=server_crt)
        logger.info('SSL connection established. Two-way SSL authentication '
                    'completed successfully.')
      except ssl.SSLError as err:
        logger.error('Two-way SSL authentication failed. Ensure that '
                    'server and agent certificates were signed by the same CA '
                    'and restart the agent. '
                    '\nIn order to receive a new agent certificate, remove '
                    'existing certificate file from keys directory. As a '
                    'workaround you can turn off two-way SSL authentication in '
                    'server configuration(ambari.properties) '
                    '\nExiting..')
        raise err

  def create_connection(self):
    if self.sock:
      self.sock.close()
    logger.info("SSL Connect being called.. connecting to the server")
    sock = socket.create_connection((self.host, self.port), 60)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    if self._tunnel_host:
      self.sock = sock
      self._tunnel()

    return sock

class CachedHTTPSConnection:
  """ Caches a ssl socket and uses a single https connection to the server. """
  
  def __init__(self, config):
    self.connected = False;
    self.config = config
    self.server = config.get('server', 'hostname')
    self.port = config.get('server', 'secured_url_port')
    self.connect()
  
  def connect(self):
    if  not self.connected:
      self.httpsconn = VerifiedHTTPSConnection(self.server, self.port, self.config)
      self.httpsconn.connect()
      self.connected = True
    # possible exceptions are caught and processed in Controller


  
  def forceClear(self):
    self.httpsconn = VerifiedHTTPSConnection(self.server, self.port, self.config)
    self.connect()
    
  def request(self, req): 
    self.connect()
    try:
      self.httpsconn.request(req.get_method(), req.get_full_url(),
                                  req.get_data(), req.headers)
      response = self.httpsconn.getresponse()
      readResponse = response.read()
    except Exception as ex:
      # This exception is caught later in Controller
      logger.debug("Error in sending/receving data from the server " + 
                   traceback.format_exc())
      logger.info("Encountered communication error. Details: " + repr(ex))
      self.connected = False
      raise IOError("Error occured during connecting to the server: " + str(ex))
    return readResponse
  
class CertificateManager():
  def __init__(self, config):
    self.config = config
    self.keysdir = self.config.get('security', 'keysdir')
    self.server_crt = self.config.get('security', 'server_crt')
    self.server_url = 'https://' + self.config.get('server', 'hostname') + ':' \
       + self.config.get('server', 'url_port')
    
  def getAgentKeyName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + hostname.hostname() + ".key"

  def getAgentCrtName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + hostname.hostname() + ".crt"

  def getAgentCrtReqName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + hostname.hostname() + ".csr"

  def getSrvrCrtName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + "ca.crt"
    
  def checkCertExists(self):
    
    s = self.config.get('security', 'keysdir') + os.sep + "ca.crt"

    server_crt_exists = os.path.exists(s)
    
    if not server_crt_exists:
      logger.info("Server certicate not exists, downloading")
      self.loadSrvrCrt()
    else:
      logger.info("Server certicate exists, ok")
      
    agent_key_exists = os.path.exists(self.getAgentKeyName())
    
    if not agent_key_exists:
      logger.info("Agent key not exists, generating request")
      self.genAgentCrtReq()
    else:
      logger.info("Agent key exists, ok")
      
    agent_crt_exists = os.path.exists(self.getAgentCrtName())
    
    if not agent_crt_exists:
      logger.info("Agent certificate not exists, sending sign request")
      self.reqSignCrt()
    else:
      logger.info("Agent certificate exists, ok")
            
  def loadSrvrCrt(self):
    get_ca_url = self.server_url + '/cert/ca/'
    logger.info("Downloading server cert from " + get_ca_url)
    stream = urllib.request.urlopen(get_ca_url)
    response = stream.read()
    stream.close()
    srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
    srvr_crt_f.write(response)
      
  def reqSignCrt(self):
    sign_crt_req_url = self.server_url + '/certs/' + hostname.hostname()
    agent_crt_req_f = open(self.getAgentCrtReqName())
    agent_crt_req_content = agent_crt_req_f.read()
    passphrase_env_var = self.config.get('security', 'passphrase_env_var_name')
    passphrase = os.environ[passphrase_env_var]
    register_data = {'csr'       : agent_crt_req_content,
                    'passphrase' : passphrase}
    data = json.dumps(register_data)
    req = urllib.request.Request(sign_crt_req_url, data, {'Content-Type': 'application/json'})
    f = urllib.request.urlopen(req)
    response = f.read()
    f.close()
    try:
    data = json.loads(response)
    logger.debug("Sign response from Server: \n" + pprint.pformat(data))
    except Exception:
      logger.warn("Malformed response! data: " + str(data))
      data = {'result': 'ERROR'}
    result = data['result']
    if result == 'OK':
      agentCrtContent = data['signedCa']
      agentCrtF = open(self.getAgentCrtName(), "w")
      agentCrtF.write(agentCrtContent)
    else:
      # Possible exception is catched higher at Controller
      logger.error('Certificate signing failed.'
                   '\nIn order to receive a new agent'
                   ' certificate, remove existing certificate file from keys '
                   'directory. As a workaround you can turn off two-way SSL '
                   'authentication in server configuration(ambari.properties) '
                   '\nExiting..')
      raise ssl.SSLError

  def genAgentCrtReq(self):
    generate_script = GEN_AGENT_KEY % {'hostname': hostname.hostname(),
                                     'keysdir' : self.config.get('security', 'keysdir')}
    logger.info(generate_script)
    p = subprocess.Popen([generate_script], shell=True, stdout=subprocess.PIPE)
    p.communicate()
      
  def initSecurity(self):
    self.checkCertExists()
