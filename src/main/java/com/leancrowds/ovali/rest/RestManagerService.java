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

import com.leancrowds.ovali.OvaliAbstractService;
import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.users.OvaliAuthHandler;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;

public class RestManagerService extends OvaliAbstractService<RestManagerInterface> {
	private static final Logger logger = LoggerFactory.getLogger(RestManagerService.class);
	public RestManagerService(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
	}
	@Override
	public String getServiceType() {
		return "rest-manager";
	}
	@Override
	public void serviceInit(AsyncResultHandler<JsonObject> handler) {
		logger.info("serviceInit");
		if (concreteService == null) { 
			handler.handle(Future.failedFuture("concrete rest is null"));
			return;
		}
		sm.getRouter().post("/rest/*").handler(BodyHandler.create());
		sm.getRouter().put("/rest/*").handler(BodyHandler.create());
		sm.getRouter().patch("/rest/*").handler(BodyHandler.create());
		
		sm.getRouter().route("/rest/*").handler(new OvaliAuthHandler(sm.getUsersManagerService()));
		sm.getRouter().route("/rest/*").handler(routingContext -> {
			JsonObject restContext = new JsonObject();
			if (routingContext.user() != null)
				restContext = new JsonObject().put("user",routingContext.user().principal());			
			concreteService.act(routingContext, restContext, res -> {
				if (!res.succeeded()) {
					logger.error(res.result().encodePrettily());
					routingContext.response().setStatusCode(500).putHeader("content-type", "text/plain").end(res.cause().getMessage());
					return;
				}
				if (res.result().getString("status").equals("error")) {
					logger.warn(res.result().encodePrettily());
					routingContext.response().setStatusCode(res.result().getInteger("statusCode")).putHeader("content-type", "text/plain").end(res.result().getString("message"));
				} else if (res.result().getString("status").equals("ok") && res.result().getBoolean("created") != null && res.result().getBoolean("created") == true) {
					String id = new String(res.result().getJsonObject("doc").getString("@rid"));
					routingContext.response().setStatusCode(201).putHeader("Location", routingContext.request().uri() + "/" + id.substring(1)).
					  putHeader("content-type", "application/json").end(res.result().encode());
				} else {
					if (routingContext.request().params().get("envelop") == null && res.result().getJsonArray("docs") != null)
						routingContext.response().putHeader("content-type", "application/json").end(res.result().getJsonArray("docs").encode());
					else if (routingContext.request().params().get("envelop") == null && res.result().getJsonObject("doc") != null)
						routingContext.response().putHeader("content-type", "application/json").end(res.result().getJsonObject("doc").encode());
					else routingContext.response().putHeader("content-type", "application/json").end(res.result().encode());
				}
			});
		});		
		super.serviceInit(handler);
	}
}
