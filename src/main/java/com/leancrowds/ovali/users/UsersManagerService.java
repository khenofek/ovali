package com.leancrowds.ovali.users;

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

import com.leancrowds.ovali.OvaliAbstractService;
import com.leancrowds.ovali.OvaliJsonValidator;
import com.leancrowds.ovali.OvaliServiceManager;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class UsersManagerService extends OvaliAbstractService<UsersManagerInterface> {
	private static final Logger logger = LoggerFactory.getLogger(UsersManagerService.class);
	Map<String,OvaliJsonValidator> validators = new HashMap<String,OvaliJsonValidator>();
	
	public UsersManagerService(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
		validators.put("users-login", new OvaliJsonValidator(config.getJsonObject("schema").getJsonObject("users-login").toString(), sm.getVertx()));
		validators.put("authorise", new OvaliJsonValidator(config.getJsonObject("schema").getJsonObject("authorise").toString(), sm.getVertx()));
		validators.put("logout", new OvaliJsonValidator(config.getJsonObject("schema").getJsonObject("logout").toString(), sm.getVertx()));
		validators.put("register-anon", new OvaliJsonValidator(config.getJsonObject("schema").getJsonObject("register-anon").toString(), sm.getVertx()));
		validators.put("register-mobile", new OvaliJsonValidator(config.getJsonObject("schema").getJsonObject("register-mobile").toString(), sm.getVertx()));
	}
	@Override
	public String getServiceType() {
		return "users-manager";
	}
	@Override
	public void serviceInit(AsyncResultHandler<JsonObject> handler) {
		sm.getRouter().post("/users/*").handler(this::checkHttpsOnOpenshift);
		sm.getRouter().post("/users/*").handler(BodyHandler.create());		
		sm.getRouter().post("/users/login").handler(this::login);
		sm.getRouter().post("/users/logout").handler(this::logout);
		sm.getRouter().post("/users/register").handler(new OvaliAuthHandler(sm.getUsersManagerService()));
		sm.getRouter().post("/users/register").handler(this::register);
		sm.getRouter().get("/users/activate/:registrationID").handler(this::activate);
		sm.getRouter().post("/users/registerMobile").handler(this::registerMobile);
		sm.getRouter().post("/users/authorise").handler(this::authorise);
		sm.getRouter().post("/users/new-password").handler(this::newPassword);
		sm.getRouter().post("/users/forgot-password-mobile").handler(this::forgotPassword);
		
		sm.getRouter().get("/users/checkUUID").handler(this::checkUUID);
		sm.getRouter().get("/users/whosOnline").handler(this::whosOnline);
		super.serviceInit(handler);
	}	
	static public boolean isAdmin(JsonObject user) {
		io.vertx.core.json.JsonArray roles = user.getJsonArray("roles");
		if (roles != null && roles.contains("admin")) return true;
		return false;		
	}

	public void login(RoutingContext routingContext) {
		logger.trace("Got body: " + routingContext.getBodyAsJson());
		validators.get("users-login").validate(routingContext.getBodyAsString(), res -> {
			if (res.failed()) {
				routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
				return;
			}			
			if (concreteService != null) concreteService.login(routingContext.getBodyAsJson(), loginRes -> {
				if (loginRes.failed()) {
					routingContext.response().setStatusCode(500).end("500 - " + loginRes.cause().getMessage());
					return;
				}			
				if (loginRes.result().getString("status").equals("ok"))
					routingContext.response().putHeader("content-type", "application/json").end(loginRes.result().encode());
				else if (loginRes.result().getString("status").equals("denied")) 
					routingContext.response().setStatusCode(401).end("401 - Access Denied");
				else if (loginRes.result().getString("message").equals("validation error"))
					routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
			});			
		});
	}
	public void logout(RoutingContext routingContext) {
		validators.get("authorise").validate(routingContext.getBodyAsString(), res -> {
			if (res.failed()) {
				routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
				return;
			}						
			if (concreteService != null) concreteService.logout(routingContext.getBodyAsJson().getString("sessionID"), logoutRes -> {
				routingContext.response().putHeader("content-type", "application/json").end(logoutRes.result().encode());
			});
		});		
	}
	public void register(RoutingContext routingContext) {
		logger.trace("Got body: " + routingContext.getBodyAsJson());
		validators.get("register-anon").validate(routingContext.getBodyAsString(), res -> {
			if (res.failed()) {
				routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
				return;
			}									
			JsonObject options = new JsonObject();
			if (routingContext.user() != null && routingContext.user().principal() != null && UsersManagerService.isAdmin(routingContext.user().principal())) {
				options.put("activationNeeded", false);				
			} else options.put("activationNeeded", true);
			if (concreteService != null) concreteService.register(routingContext.getBodyAsJson(), options, regRes -> {
				if (regRes.result().getString("status").equals("error")) {
					routingContext.response().setStatusCode(regRes.result().getInteger("statusCode")).putHeader("content-type", "application/json").
					end(regRes.result().encode());
				} else routingContext.response().setStatusCode(201).putHeader("content-type", "application/json").end(regRes.result().encode());
			});
		});
	}
	public void activate(RoutingContext routingContext) {
		if (concreteService != null) concreteService.activate(routingContext.request().getParam("registrationID"), activateRes -> {
			if (activateRes.result().getString("status").equals("error")) {
				routingContext.response().setStatusCode(activateRes.result().getInteger("statusCode")).putHeader("content-type", "application/json").
				end(activateRes.result().encode());
			} else if (activateRes.result().getString("status").equals("ok") && activateRes.result().getBoolean("redirect").equals(true)) {
				routingContext.response().setStatusCode(303).putHeader("Location", activateRes.result().getString("Location")).end("303 See Other");
			} else routingContext.response().putHeader("content-type", "application/json").end(activateRes.result().getString("reply"));
		});		
	}
	public void registerMobile(RoutingContext routingContext) {
		logger.trace("Got body: " + routingContext.getBodyAsJson());
		validators.get("register-mobile").validate(routingContext.getBodyAsString(), res -> {
			if (res.failed()) {
				routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
				return;
			}									
			if (concreteService != null) concreteService.register(routingContext.getBodyAsJson(), new JsonObject().put("activationNeeded", false), regRes -> {
				if (regRes.result().getString("status").equals("error")) {
					routingContext.response().setStatusCode(regRes.result().getInteger("statusCode")).putHeader("content-type", "application/json").
					end(regRes.result().encode());
				} else routingContext.response().setStatusCode(201).putHeader("content-type", "application/json").end(regRes.result().encode());
			});
		});
	}	
	public void authorise(RoutingContext routingContext) {
		validators.get("authorise").validate(routingContext.getBodyAsString(), res -> {
			if (res.failed()) {
				routingContext.response().setStatusCode(400).end("400 (Bad Request) - Validation Error");
				return;
			}						
			if (concreteService != null) concreteService.authenticateLoginSessionID(routingContext.getBodyAsJson().getString("sessionID"), authorizeRes -> {
				routingContext.response().putHeader("content-type", "application/json").end(authorizeRes.result().encode());
			});			
		});
	}	
	public void whosOnline(RoutingContext routingContext) {
		concreteService.whosOnline(res -> {
			routingContext.response().putHeader("content-type", "application/json").end(res.result().encode());
		});
	}
	public void checkUUID(RoutingContext routingContext) {
		concreteService.checkUUID(routingContext.request().params().get("uuid"), res -> {
			routingContext.response().putHeader("content-type", "application/json").end(res.result().encode());
		});
	}	
	public void newPassword(RoutingContext routingContext) {
		JsonObject body = routingContext.getBodyAsJson();
		logger.info("newPassword::Got body: " + body.encodePrettily());
		concreteService.newPassword(body.getString("id"), body.getString("password"), res -> {
			if (res.result().getString("status").equals("ok"))
				routingContext.response().putHeader("content-type", "application/json").end(res.result().encode());
			else
				routingContext.response().setStatusCode(res.result().getInteger("statusCode")).putHeader("content-type", "text/plain").end(res.result().getString("message"));
		});
	}
	public void forgotPassword(RoutingContext routingContext) {
		JsonObject body = routingContext.getBodyAsJson();
		logger.info("newPassword::Got body: " + body.encodePrettily());
		concreteService.forgotPassword(body, res -> {
			if (res.result().getString("status").equals("ok"))
				routingContext.response().putHeader("content-type", "application/json").end(res.result().encode());
			else
				routingContext.response().setStatusCode(res.result().getInteger("statusCode")).putHeader("content-type", "text/plain").end(res.result().getString("message"));
		});
	}	
}
