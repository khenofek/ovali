package com.leancrowds.ovali.orient;

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

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;

public class OrientPersistorService extends OvaliAbstractService<OrientPersistorInterface> {
	private static final Logger logger = LoggerFactory.getLogger(OrientPersistorService.class);
	
	public OrientPersistorService(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
	}
	@Override
	public void serviceInit(AsyncResultHandler<JsonObject> handler) {
		logger.info("serviceInit");
		sm.getRouter().post("/services/" + getServiceType() + "/act").handler(BodyHandler.create());
		sm.getRouter().post("/services/" + getServiceType() + "/act").handler(ctx -> {
			if (ctx.request().headers().get("OVALI_SIGNATURE") == null || !ctx.request().headers().get("OVALI_SIGNATURE").equals("35AC678DF")) {
				ctx.response().setStatusCode(401).putHeader("content-type", "text/plain").end("401 - denied");
				return;
			}
			concreteService.act(ctx.getBodyAsJson(), res -> {
				ctx.response().end(res.result().encode());
			});
		});
		getConcreteService("1").init();
		super.serviceInit(handler);
	}	
	@Override
	public String getServiceType() {
		return "orient-persistor";
	}
}
