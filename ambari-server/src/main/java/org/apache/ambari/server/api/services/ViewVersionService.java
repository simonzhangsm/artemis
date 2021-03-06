/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for view version resource requests.
 */
public class ViewVersionService extends BaseService {
	
	/**
	 * Parent view name.
	 */
	private final String viewName;
	
	// ----- Constructors ------------------------------------------------------
	
	/**
	 * Construct a view version service.
	 * 
	 * @param viewName the view name
	 */
	public ViewVersionService(String viewName) {
		this.viewName = viewName;
	}
	
	// ----- ViewVersionService ------------------------------------------------
	
	/**
	 * Handles: GET /versions/{version} Get a specific view version.
	 * 
	 * @param headers http headers
	 * @param ui uri info
	 * @param version version id
	 * @return view instance representation
	 */
	@GET
	@Path("{version}")
	@Produces("text/plain")
	public Response getVersions(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("version") String version) {
		
		return handleRequest(headers, null, ui, Request.Type.GET, createResource(viewName, version));
	}
	
	/**
	 * Handles: GET /versions Get all views versions.
	 * 
	 * @param headers http headers
	 * @param ui uri info
	 * @return view collection resource representation
	 */
	@GET
	@Produces("text/plain")
	public Response getVersions(@Context HttpHeaders headers, @Context UriInfo ui) {
		
		return handleRequest(headers, null, ui, Request.Type.GET, createResource(viewName, null));
	}
	
	/**
	 * Handles: POST /versions/{version} Create a specific view version.
	 * 
	 * @param headers http headers
	 * @param ui uri info
	 * @param version the version
	 * @return information regarding the created view
	 */
	@POST
	@Path("{version}")
	@Produces("text/plain")
	public Response createVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("version") String version) {
		
		return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version));
	}
	
	/**
	 * Handles: PUT /versions/{version} Update a specific view version.
	 * 
	 * @param headers http headers
	 * @param ui uri info
	 * @param version the version
	 * @return information regarding the updated view
	 */
	@PUT
	@Path("{version}")
	@Produces("text/plain")
	public Response updateVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("version") String version) {
		
		return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version));
	}
	
	/**
	 * Handles: DELETE /versions/{version} Delete a specific view version.
	 * 
	 * @param headers http headers
	 * @param ui uri info
	 * @param version version id
	 * @return information regarding the deleted view version
	 */
	@DELETE
	@Path("{version}")
	@Produces("text/plain")
	public Response deleteVersions(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("version") String version) {
		
		return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(viewName, version));
	}
	
	/**
	 * Get the instances sub-resource
	 * 
	 * @param version the version
	 * @return the instance service
	 */
	@Path("{version}/instances")
	public ViewInstanceService getInstanceHandler(@PathParam("version") String version) {
		
		return new ViewInstanceService(viewName, version);
	}
	
	// ----- helper methods ----------------------------------------------------
	
	/**
	 * Create a view resource.
	 * 
	 * @param viewName view name
	 * @return a view resource instance
	 */
	private ResourceInstance createResource(String viewName, String version) {
		Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
		mapIds.put(Resource.Type.View, viewName);
		mapIds.put(Resource.Type.ViewVersion, version);
		return createResource(Resource.Type.ViewVersion, mapIds);
	}
}
