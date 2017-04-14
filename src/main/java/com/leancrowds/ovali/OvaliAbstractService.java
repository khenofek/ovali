package com.leancrowds.ovali;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.leancrowds.ovali.users.OvaliAuthHandler;
import com.leancrowds.ovali.users.UsersManagerService;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public abstract class OvaliAbstractService<ConcreteServiceInterface> implements OvaliServiceInterface {
	private static final Logger logger = LoggerFactory.getLogger(OvaliAbstractService.class);
	protected OvaliServiceManager sm = null;
	protected ConcreteServiceInterface concreteService = null;
	protected JsonObject config;
	protected String version = "1";

	public OvaliAbstractService(OvaliServiceManager sm, JsonObject config) {
		logger.trace("OvaliAbstractService constructor");
		this.sm = sm;
		if (config != null) {
			logger.trace("config = " + config.encodePrettily());
			updateConfig(config);
		}
	}

	public ConcreteServiceInterface getConcreteService(String serviceVersion) {
		if (serviceVersion.equals(version)) return concreteService;
		else return null;
	}
	@Override
	public void serviceInit(AsyncResultHandler<JsonObject> handler) {
		sm.getRouter().put("/services/" + getServiceType() + "/config").handler(BodyHandler.create());
		sm.getRouter().put("/services/" + getServiceType() + "/config").handler(new OvaliAuthHandler(sm.getUsersManagerService()));
		sm.getRouter().put("/services/" + getServiceType() + "/config").handler(ctx -> {
			if (ctx.user() == null || !UsersManagerService.isAdmin(ctx.user().principal())) {
				ctx.response().setStatusCode(401).putHeader("content-type", "text/plain").end("401 - denied");
				return;
			}
			updateConfig(ctx.getBodyAsJson());
			ctx.response().putHeader("content-type", "text/html").end("Config updated successfully");
		});
		sm.getRouter().get("/services/" + getServiceType() + "/config").handler(new OvaliAuthHandler(sm.getUsersManagerService()));
		sm.getRouter().get("/services/" + getServiceType() + "/config").handler(ctx -> {
			if (ctx.user() == null || !UsersManagerService.isAdmin(ctx.user().principal())) {
				ctx.response().setStatusCode(401).putHeader("content-type", "text/plain").end("401 - denied");
				return;
			}			
			ctx.response().putHeader("content-type", "application/json").end(this.config.encodePrettily());
		});		
		handler.handle(Future.succeededFuture());
	}	
	public void checkHttpsOnOpenshift(RoutingContext routingContext) {
		final String openshiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
		if (openshiftVertxIP != null && routingContext.request().headers().get("X-Forwarded-Proto") != null &&
				!routingContext.request().headers().get("X-Forwarded-Proto").equals("https")) {
			routingContext.response().setStatusCode(403).putHeader("content-type", "text/plain").end("403 - Forbidden");
		} else routingContext.next();
	}
		
	@SuppressWarnings("unchecked")
	@Override
	public void updateConfig(JsonObject config) {
		this.config = config;
		JsonObject versionConfig = config.getJsonObject(version);
		String impClass = versionConfig.getString("implementation");
		try {
			Class<?> myClass = Class.forName(impClass);
			Class<?>[] types = {OvaliServiceManager.class, JsonObject.class};
			Constructor<?> constructor = myClass.getConstructor(types);			
			Object[] parameters = {sm, versionConfig.getJsonObject(impClass)};
			this.concreteService = (ConcreteServiceInterface) constructor.newInstance(parameters);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
