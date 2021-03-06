/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.ambari.server.controller;

import org.apache.ambari.server.state.AutoDeployInfo;

/**
 * Stack service component response.
 */
public class StackServiceComponentResponse {
	/**
	 * stack name
	 */
	private String stackName;
	
	/**
	 * stack version
	 */
	private String stackVersion;
	
	/**
	 * service name
	 */
	private String serviceName;
	
	/**
	 * component name
	 */
	private String componentName;
	
	/**
	 * component category
	 */
	private String componentCategory;
	
	/**
	 * is component a client component
	 */
	private boolean isClient;
	
	/**
	 * is component a master component
	 */
	private boolean isMaster;
	
	/**
	 * cardinality requirement
	 */
	private String cardinality;
	
	/**
	 * auto deploy information
	 */
	private AutoDeployInfo autoDeploy;
	
	public StackServiceComponentResponse(String componentName, String componentCategory, boolean isClient, boolean isMaster, String cardinality) {
		setComponentName(componentName);
		setComponentCategory(componentCategory);
		setClient(isClient);
		setMaster(isMaster);
		setCardinality(cardinality);
	}
	
	/**
	 * Get stack name.
	 * 
	 * @return stack name
	 */
	public String getStackName() {
		return stackName;
	}
	
	/**
	 * Set stack name.
	 * 
	 * @param stackName stack name
	 */
	public void setStackName(String stackName) {
		this.stackName = stackName;
	}
	
	/**
	 * Get stack version.
	 * 
	 * @return stack version
	 */
	public String getStackVersion() {
		return stackVersion;
	}
	
	/**
	 * Set stack version.
	 * 
	 * @param stackVersion stack version
	 */
	public void setStackVersion(String stackVersion) {
		this.stackVersion = stackVersion;
	}
	
	/**
	 * Get service name.
	 * 
	 * @return service name
	 */
	public String getServiceName() {
		return serviceName;
	}
	
	/**
	 * Set service name.
	 * 
	 * @param serviceName service name
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	/**
	 * Get component name.
	 * 
	 * @return component name
	 */
	public String getComponentName() {
		return componentName;
	}
	
	/**
	 * Set component name.
	 * 
	 * @param componentName component name
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	
	/**
	 * Get component category.
	 * 
	 * @return component category
	 */
	public String getComponentCategory() {
		return componentCategory;
	}
	
	/**
	 * Set the component category.
	 * 
	 * @param componentCategory component category
	 */
	public void setComponentCategory(String componentCategory) {
		this.componentCategory = componentCategory;
	}
	
	/**
	 * Determine whether the component is a client component.
	 * 
	 * @return whether the component is a client component
	 */
	public boolean isClient() {
		return isClient;
	}
	
	/**
	 * Set whether the component is a client component.
	 * 
	 * @param isClient whether the component is a client
	 */
	public void setClient(boolean isClient) {
		this.isClient = isClient;
	}
	
	/**
	 * Determine whether the component is a master component.
	 * 
	 * @return whether the component is a master component
	 */
	public boolean isMaster() {
		return isMaster;
	}
	
	/**
	 * Set whether the component is a master component.
	 * 
	 * @param isMaster whether the component is a master
	 */
	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}
	
	/**
	 * Get cardinality requirement of component.
	 * 
	 * @return component cardinality requirement
	 */
	public String getCardinality() {
		return cardinality;
	}
	
	/**
	 * Set component cardinality requirement.
	 * 
	 * @param cardinality cardinality requirement
	 */
	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}
	
	/**
	 * Get auto deploy information.
	 * 
	 * @return auto deploy information
	 */
	public AutoDeployInfo getAutoDeploy() {
		return autoDeploy;
	}
	
	/**
	 * Set auto deploy information.
	 * 
	 * @param autoDeploy auto deploy info
	 */
	public void setAutoDeploy(AutoDeployInfo autoDeploy) {
		this.autoDeploy = autoDeploy;
	}
}
