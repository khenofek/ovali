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

import java.text.ParseException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.orient.OrientPersistorBatch;
import com.leancrowds.ovali.rest.PersistentResourceManager;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UsersResourceManager extends PersistentResourceManager {
	private static final Logger logger = LoggerFactory.getLogger(UsersResourceManager.class);

	public UsersResourceManager(OvaliServiceManager sm, JsonObject config) {
		super("users",sm,config);
		logger.info("config: " + config);		
	}
	@Override
	protected boolean isAuthorized(String restAction, JsonObject context) {
		if (((context.getJsonObject("doc") != null && context.getJsonObject("doc").getString("newPassword") != null) ||
				(context.getJsonObject("doc") != null && context.getJsonObject("doc").getBoolean("isActive") != null)) &&
				context.getJsonObject("user") != null && UsersManagerService.isAdmin(context.getJsonObject("user"))) return true;
		else return super.isAuthorized(restAction, context);
	}
	static public JsonObject generateHashedPassword(String password, Integer iterations) {
		String saltedHash[] = null;
		try {
			saltedHash = PasswordUtil.getSaltedHash(password,iterations); 
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final String hashedPassword = saltedHash[0];
		final String salt = saltedHash[1];
		JsonObject hashedPasswordJson = new JsonObject();
		hashedPasswordJson.put("iterations", iterations);
		hashedPasswordJson.put("salt", salt);
		hashedPasswordJson.put("hashedPassword", hashedPassword);
		return hashedPasswordJson;
	}
	protected OrientPersistorBatch generateUserData(JsonObject doc, JsonObject context) {
		LocalDateTime nowInUtc = LocalDateTime.now(Clock.systemUTC());
		JsonObject passwordParams = generateHashedPassword(doc.getString("password"), config.getInteger("passwordHashIterations"));
		passwordParams.put("username", doc.getString("username").toUpperCase());
		passwordParams.put("created", nowInUtc.format(DateTimeFormatter.ISO_DATE_TIME));
		if ((context.getJsonObject("options") != null && context.getJsonObject("options").getBoolean("activationNeeded")) ||
			(context.getJsonObject("user") != null) && !UsersManagerService.isAdmin(context.getJsonObject("user"))) {
			passwordParams.put("isActivatedByUser", false);
			passwordParams.put("isActive", false);	
			final String registrationID = UUID.randomUUID().toString();
			passwordParams.put("registrationID", registrationID);
		} else passwordParams.put("isActive", true);
		OrientPersistorBatch batch = new OrientPersistorBatch();
		batch.create("passwordParams", passwordParams);
		
		doc.put("created", nowInUtc.format(DateTimeFormatter.ISO_DATE_TIME));
		doc.put("roles", new JsonArray()).remove("password");		
		batch.create("users", doc);
		return batch;
	}		
	protected OrientPersistorBatch generateDuplicateTest(JsonObject doc, JsonObject context) {
		OrientPersistorBatch batch = new OrientPersistorBatch();
		batch.cmd("select from users where username matches '(?i)" + doc.getString("username") + "'");
		if (doc.containsKey("email"))
			batch.cmd("select from users where email = '" + doc.getString("email") + "'");
		if (doc.containsKey("uuid"))
			batch.cmd("select from users where uuid = '" + doc.getString("uuid") + "'");
		return batch;		
	}
	@Override
	public void create(JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {		
		logger.trace("context: " + context.encodePrettily());
		OrientPersistorBatch duplicateTestBatch = generateDuplicateTest(doc, context);
		sm.getOrientPersistorService().doBatch(duplicateTestBatch, duplicateTestRes -> {
			int usersNumber = duplicateTestRes.result().getJsonArray("replies").getJsonObject(0).getJsonArray("docs").size();
			if (usersNumber > 0) {
				JsonObject resJson = new JsonObject().put("status", "error").put("statusCode",409).put("message", "409 CONFLICT - duplicate username");
				handler.handle(Future.succeededFuture(resJson));
				return;
			}
			int nextTest = 1;
			if (doc.containsKey("email")) {
				int emailsNumber = duplicateTestRes.result().getJsonArray("replies").getJsonObject(nextTest).getJsonArray("docs").size();
				if (emailsNumber > 0) {
					JsonObject resJson = new JsonObject().put("status", "error").put("statusCode",409).put("message", "409 CONFLICT - duplicate email");
					handler.handle(Future.succeededFuture(resJson));
					return;
				}
				nextTest += 1;
			}
			if (doc.containsKey("uuid")) {
				int uuidNumber = duplicateTestRes.result().getJsonArray("replies").getJsonObject(nextTest).getJsonArray("docs").size();
				if (uuidNumber > 0) {
					JsonObject resJson = new JsonObject().put("status", "error").put("statusCode",409).put("message", "409 CONFLICT - duplicate device");
					handler.handle(Future.succeededFuture(resJson));
					return;
				}
			}			
			OrientPersistorBatch userDataBatch = generateUserData(doc, context);
			sm.getOrientPersistorService().doBatch(userDataBatch, userDataRes -> {
				logger.trace("Got batch: " + userDataRes.result().encodePrettily());
				JsonObject reply = new JsonObject().put("status", "ok").put("user", userDataRes.result().getJsonArray("replies").getJsonObject(1)).
						put("passwordParam",userDataRes.result().getJsonArray("replies").getJsonObject(0));
				handler.handle(Future.succeededFuture(reply));
			});			
		});
	}
	@Override
	public void update(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		logger.info("update: " + doc.encodePrettily());
		if ((doc.getString("newPassword") != null || doc.getBoolean("isActive") != null) && UsersManagerService.isAdmin(context.getJsonObject("user"))) {
			sm.getOrientPersistorService().find("passwordParams", new JsonObject().put("username", doc.getString("username").toUpperCase()), ppRes -> {
				if (ppRes.result().getJsonArray("docs").size() == 0)
					handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode", 400).put("message", "user not found")));
				JsonObject passwordParams = new JsonObject();
				if (doc.getString("newPassword") != null && !doc.getString("newPassword").equals(""))
					passwordParams = generateHashedPassword(doc.getString("newPassword"),config.getInteger("passwordHashIterations"));					
				if (doc.getBoolean("isActive") != null)
					passwordParams.put("isActive", doc.getBoolean("isActive"));
				sm.getOrientPersistorService().update(ppRes.result().getJsonArray("docs").getJsonObject(0).getString("@rid"), passwordParams, updateRes -> {
					handler.handle(Future.succeededFuture(updateRes.result()));
				});
			});
		} else super.update(ID, doc, context, handler);
	}		
	@Override
	public void read(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler) {		
		if (context.getJsonObject("params").getString("userActivations") != null) {
			super.read(ID, context, res -> {
				sm.getOrientPersistorService().cmd("select username,isActivatedByUser,isActive from passwordParams where username = " +
						res.result().getJsonObject("doc").getString("username").toUpperCase(), resActivations -> {
					JsonObject combinedRes = res.result().copy();
					combinedRes.getJsonObject("doc").put("userActivations", resActivations.result().getJsonArray("docs").getJsonObject(0));
					logger.trace("result: " + combinedRes);
					handler.handle(Future.succeededFuture(combinedRes));
				});
			});
		} else super.read(ID, context, handler);
	}	
	@Override
	public void query(MultiMap query, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		if (query.get("usersActivations") != null) {
			sm.getOrientPersistorService().cmd("select username,isActivatedByUser,isActive from passwordParams", res -> {
				handler.handle(Future.succeededFuture(res.result()));
			});
		} else super.query(query, context, handler);
	}	
}
