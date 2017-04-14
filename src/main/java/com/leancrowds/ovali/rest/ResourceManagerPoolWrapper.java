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

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public class ResourceManagerPoolWrapper implements ResourceManagerInterface {	
	public ResourceManagerPoolWrapper(String resource, GenericKeyedObjectPool<String, ResourceManagerInterface> rmPools) {
		this.resource = resource;
		this.rmPools = rmPools;
	}
	String resource;
	GenericKeyedObjectPool<String, ResourceManagerInterface> rmPools;

	@Override
	public void isAuthorizedAndValidated(String restAction, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.create(doc, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
	@Override
	public void read(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.read(ID, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
	@Override
	public void query(MultiMap query, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.query(query, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
	@Override
	public void update(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.update(ID, doc, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
	@Override
	public void override(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.override(ID, doc, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
	@Override
	public void delete(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		try {
			ResourceManagerInterface rm = rmPools.borrowObject(resource);
			if (rm != null)
				rm.delete(ID, context, res -> {
					rmPools.returnObject(resource, rm);
					handler.handle(Future.succeededFuture(res.result()));
				});
			else handler.handle(Future.failedFuture("No Resource Manager"));
		} catch (Exception e) {
			e.printStackTrace();
			handler.handle(Future.failedFuture(e));
		}
	}
}
