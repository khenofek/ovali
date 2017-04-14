package com.leancrowds.ovali.rest;

/*
 * Copywrite 2017 Khen Ofek
 * 
 * This file is part of Ovali.

    Ovali is free software: you can redistribute it and/or modify
    it under the terms of the Affero GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Ovali is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Affero GNU General Public License for more details.

    You should have received a copy of the Affero GNU General Public License
    along with Ovali.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import com.leancrowds.ovali.OvaliServiceManager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class RestManagerReal implements RestManagerInterface {
	private static final Logger logger = LoggerFactory.getLogger(RestManagerReal.class);
	protected JsonObject config = null;
	protected OvaliServiceManager sm;
	protected GenericKeyedObjectPool<String, ResourceManagerInterface> rmPools = null;

	public static class RestMap {
		private Map<String,String> parents = new HashMap<String,String>();
		private String resource = null;
		private String id = null;
		public RestMap(String path) {
			String splittedPath[] = path.split("/");
			for (String e:splittedPath) {
				if (resource == null) resource = e;
				else {
					if (id == null) id = e;
					else {
						parents.put(resource, '#' + id);
						resource = e;
						id = null;
					}
				}
			}
		}
		public String getResource() { return resource; }
		public String getId() { return id; }
		public Map<String,String> getParents() { return parents; }
		public String toString() { 
			String ret = "resource: " + resource + " id: " + id + "\n";
			ret += "parents:\n";
			for (Map.Entry<String, String> e:parents.entrySet()) {
				ret += e.getKey() + " -- " + e.getValue() + "\n";
			}
			return ret;
		}
		public JsonObject toJsonObject() {
			JsonObject res = new JsonObject().put("resource", resource);
			if (id != null) res.put("id", id);
			if (parents != null) res.put("parents", parents);
			return res;
		}
	}

	public RestManagerReal(OvaliServiceManager sm, JsonObject config) {
		logger.info("constructor config = " + config.encodePrettily());
		this.sm = sm;
		this.config = config;		
		rmPools = new GenericKeyedObjectPool<String, ResourceManagerInterface>(new ResourceManagersPools(sm,config.getJsonObject("resources")));
		rmPools.setBlockWhenExhausted(false);
	}
	@Override
	public ResourceManagerInterface getResourceManager(String resource) {
		return new ResourceManagerPoolWrapper(resource, rmPools);
	}	
	@Override
	public void act(RoutingContext routeCtx, JsonObject restContext, Handler<AsyncResult<JsonObject>> res) {		
		RestMap restMap = new RestMap(routeCtx.request().path().substring(6));				
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(restMap.getResource());
			if (rm != null) {
				String restAction = null;
				JsonObject restBody = null;
				if (routeCtx.fileUploads().size() > 0) {
					restAction = routeCtx.request().getParam("restAction");
					restBody = new JsonObject(routeCtx.request().getFormAttribute("restBody"));
				} else {
					if (routeCtx.request().method().equals(HttpMethod.GET) && restMap.getId() != null) restAction = "read";
					if (routeCtx.request().method().equals(HttpMethod.GET) && restMap.getId() == null) restAction = "query";
					if (routeCtx.request().method().equals(HttpMethod.POST) && restMap.getId() == null) restAction = "create";
					if (routeCtx.request().method().equals(HttpMethod.PATCH) && restMap.getId() != null) restAction = "update";				
					if (routeCtx.request().method().equals(HttpMethod.PUT) && restMap.getId() != null) restAction = "override";
					if (routeCtx.request().method().equals(HttpMethod.DELETE) && restMap.getId() != null) restAction = "delete";
					restBody = routeCtx.getBodyAsJson();
				}
				final String action = restAction;
				final JsonObject body = restBody;
				populateRestContext(restMap, routeCtx, restContext, body);
				rm.isAuthorizedAndValidated(restAction, restContext, permittedRes -> {								
					logger.info("restContext: " + restContext.encodePrettily());
					if (permittedRes.result().getString("status").equals("ok"))
						switch (action) {
						case "read":
							rm.read(restMap.getId(), restContext, rmRes -> {
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
						case "query":
							rm.query(routeCtx.request().params(), restContext, rmRes -> {
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
						case "create":
							rm.create(body, restContext, rmRes -> {
								if (rmRes.result().getString("status").equals("ok"))
									rmRes.result().put("created", true);
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
						case "update":
							rm.update(restMap.getId(), body, restContext, rmRes -> {
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
						case "override":
							rm.override(restMap.getId(), body, restContext, rmRes -> {
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
						case "delete":
							rm.delete(restMap.getId(), restContext, rmRes -> {
								logger.trace("Got rmRes: " + rmRes.result());
								rmPools.returnObject(restMap.getResource(), rm);
								if (rmRes.succeeded())
									res.handle(Future.succeededFuture(rmRes.result()));
								else
									res.handle(Future.failedFuture(rmRes.cause()));
							});
							break;
							
						default:
							rmPools.returnObject(restMap.getResource(), rm);
							break;
						}			
					else {
						rmPools.returnObject(restMap.getResource(), rm);
						if (permittedRes.result().getString("message").equals("denied"))
							res.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",401).put("message", "401 - denied")));
						else res.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",422).put("message", "422 UNPROCESSABLE ENTITY")));
					}
				});
			} else res.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",404).put("message", "404 - Resource Not Found")));
		} catch (Exception e) {
			e.printStackTrace();
			res.handle(Future.failedFuture(e));
		}		
	}
//	private void handleRmRes(RestMap restMap, ResourceManagerInterface rm, AsyncResult<JsonObject> rmRes,
//			Handler<AsyncResult<JsonObject>> res) {
//		logger.trace("Got rmRes: " + rmRes.result());
//		rmPools.returnObject(restMap.getResource(), rm);
//		if (rmRes.succeeded())
//			res.handle(Future.succeededFuture(rmRes.result()));
//		else
//			res.handle(Future.failedFuture(rmRes.cause()));
//	}
	protected void populateRestContext(RestMap restMap, RoutingContext routeCtx, JsonObject restContext, JsonObject body) {
		restContext.put("restMap",restMap.toJsonObject());
		JsonObject params = new JsonObject();
		for (Map.Entry<String, String> entry : routeCtx.request().params().entries())
			params.put(entry.getKey(), entry.getValue());
		restContext.put("params", params);
		JsonObject headers = new JsonObject();
		for (Map.Entry<String, String> entry : routeCtx.request().headers().entries())
			headers.put(entry.getKey(), entry.getValue());
		restContext.put("headers", headers);
		JsonArray uploads = new JsonArray();
		restContext.put("doc", body);
		for (FileUpload fileUpload : routeCtx.fileUploads())
			uploads.add(new JsonObject().put("filename", fileUpload.fileName()).put("uploadedFileName",fileUpload.uploadedFileName()).
					put("fileSize",fileUpload.size()).put("contentType", fileUpload.contentType()));
		restContext.put("uploads", uploads);
	}
}
