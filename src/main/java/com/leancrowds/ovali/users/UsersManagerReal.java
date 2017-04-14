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

import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.orient.OrientPersistorBatch;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailMessage;

public class UsersManagerReal implements UsersManagerInterface {		
	private static final Logger logger = LoggerFactory.getLogger(UsersManagerReal.class);
	protected JsonObject config = null;
	protected OvaliServiceManager sm;
	
	private class UserSessionData { 
		String appVersionID;
		@SuppressWarnings("unused")
		String getAppVersionID() { return appVersionID; }
		JsonObject user; 
		JsonObject getUser() { return user; }
		public UserSessionData(String avID,JsonObject user) { appVersionID = avID; this.user = user; }
		public JsonObject toJson() {
			return new JsonObject().put("user", user).put("appVersionID", appVersionID);
		}
	}
	Map<String,UserSessionData> userSessions = new HashMap<String,UserSessionData>();

	public UsersManagerReal(OvaliServiceManager sm, JsonObject config) {
		logger.info("UsersManagerReal constructor. config = " + config.encodePrettily());
		this.sm = sm;
		this.config = config;
	}

	@Override
	public void login(JsonObject credentials, AsyncResultHandler<JsonObject> handler) {
		logger.info("Got credentials: " + credentials);
		sm.getOrientPersistorService().find("passwordParams", 
			new JsonObject().put("username", credentials.getString("username").toUpperCase()), findRes -> {
				if (!findRes.succeeded()) { 
					handler.handle(Future.failedFuture(findRes.cause().getMessage()));
					return;
				}
				if (findRes.result().getJsonArray("docs").size() == 0) {
					handler.handle(Future.succeededFuture(new JsonObject().put("status", "denied").put("message", "user not found")));
					return;
				}
				JsonObject pp = findRes.result().getJsonArray("docs").getJsonObject(0);
				if (pp.getBoolean("isActive").equals(false)) {
					handler.handle(Future.succeededFuture(new JsonObject().put("status", "denied")));
					return;
				}
				boolean passwordMatch = false;
				try {
					logger.info("pp: " + pp);
					passwordMatch = PasswordUtil.check(credentials.getString("password"), pp.getString("salt") + "$" +
							pp.getString("hashedPassword"), pp.getInteger("iterations"));
					logger.info("Password Match: " + passwordMatch);
				} catch (Exception e) {
					e.printStackTrace();
					handler.handle(Future.failedFuture(e));
				}
				if (passwordMatch) {					
					OrientPersistorBatch batch = new OrientPersistorBatch();
					batch.cmd("select from users where username matches '(?i)" + credentials.getString("username") + "'");
					if (credentials.getString("uuid") != null) batch.find("users", new JsonObject().put("uuid", credentials.getString("uuid")));
					sm.getOrientPersistorService().doBatch(batch, userRes -> {
						JsonObject user = userRes.result().getJsonArray("replies").getJsonObject(0).getJsonArray("docs").getJsonObject(0);
						String loginSessionID = UUID.randomUUID().toString();
						userSessions.put(loginSessionID, new UserSessionData("1",user));
						JsonObject result = new JsonObject().put("status","ok").put("sessionID", loginSessionID).put("username", user.getString("username")).
								put("userId",user.getString("@rid").substring(1)).put("user", user);
						if (credentials.getString("uuid") != null) {
							OrientPersistorBatch updateBatch = new OrientPersistorBatch();
							updateBatch.update(user.getString("@rid"), new JsonObject().put("uuid", credentials.getString("uuid")));
							JsonObject currentDeviceUsers = userRes.result().getJsonArray("replies").getJsonObject(1);
							if (currentDeviceUsers.getJsonArray("docs").size() == 1)
								updateBatch.update(currentDeviceUsers.getJsonArray("docs").getJsonObject(0).getString("@rid"),new JsonObject().put("uuid", "null"));
							sm.getOrientPersistorService().doBatch(updateBatch, resUpdate -> {
								handler.handle(Future.succeededFuture(result));
							});
						} else handler.handle(Future.succeededFuture(result));
					});
				} else
					handler.handle(Future.succeededFuture(new JsonObject().put("status", "denied")));
		});					
	}
	@Override
	public void logout(String loginSessionID, AsyncResultHandler<JsonObject> handler) {
		userSessions.remove(loginSessionID);
		handler.handle(Future.succeededFuture(new JsonObject().put("status", "ok")));
	}
	@Override
	public void authenticateLoginSessionID(String sessionID, AsyncResultHandler<JsonObject> handler) {
		if (userSessions.containsKey(sessionID)) {
			UserSessionData usData = userSessions.get(sessionID);
			handler.handle(Future.succeededFuture(usData.getUser()));
		} else
			handler.handle(Future.succeededFuture(new JsonObject().put("status", "denied").put("message", "session not found")));
	}

	@Override
	public void register(JsonObject userData, JsonObject options, AsyncResultHandler<JsonObject> handler) {
		if (options != null) logger.info("options: " + options.encodePrettily());
		sm.getRestManagerService().getResourceManager("users").create(userData, new JsonObject().put("options", options), handler);				
	}
	@Override
	public void activate(String registrationID, AsyncResultHandler<JsonObject> handler) {
		logger.info("activate: " + registrationID);
		sm.getOrientPersistorService().find("passwordParams", new JsonObject().put("registrationID", registrationID), res -> {
			if (res.result().getJsonArray("docs").size() == 0)
				handler.handle(Future.succeededFuture(new JsonObject().put("status","error").put("statusCode", 404).put("message", "Not Found")));
			LocalDateTime nowInUtc = LocalDateTime.now(Clock.systemUTC());
			JsonObject passwordParams = res.result().getJsonArray("docs").getJsonObject(0);
			JsonObject properties = new JsonObject().put("isActivatedByUser", true).put("isActive", true).
					put("activatedByUser", nowInUtc.format(DateTimeFormatter.ISO_DATE_TIME));
			sm.getOrientPersistorService().update(passwordParams.getString("@rid"),properties, updateRes -> {
				this.activationDone(passwordParams, handler);
			});
		});
	}
	protected void activationDone(JsonObject passwordParams, AsyncResultHandler<JsonObject> handler) {
		handler.handle(Future.succeededFuture(new JsonObject().put("status","ok").
				put("redirect", true).put("Location", sm.getBaseURL() + "/activation-done.html")));
	}

	@Override
	public void checkUUID(String uuid, AsyncResultHandler<JsonObject> handler) {
		sm.getOrientPersistorService().find("users", new JsonObject().put("uuid", uuid), res -> {
			if (res.result().getJsonArray("docs").size() == 1) {
				JsonObject user = res.result().getJsonArray("docs").getJsonObject(0);
				handler.handle(Future.succeededFuture(new JsonObject().put("username", user.getString("username"))));
			} else handler.handle(Future.succeededFuture(new JsonObject().put("message", "no user")));
		});		
	}
	@Override
	public void whosOnline(AsyncResultHandler<JsonObject> handler) {
		JsonObject sessions = new JsonObject();
		for (Map.Entry<String, UserSessionData> session : userSessions.entrySet()) {
			sessions.put(session.getKey(), session.getValue().toJson());
		}
		handler.handle(Future.succeededFuture(sessions));		
	}

	@Override
	public void newPassword(String newPasswordToken, String password, AsyncResultHandler<JsonObject> handler) {		
		sm.getOrientPersistorService().find("users", new JsonObject().put("newPasswordID", newPasswordToken), resUser -> {
			if (resUser.result().getJsonArray("docs").size() == 0) { 
				handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",404).put("message", "ID Not Found")));
				return;
			}
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            JsonObject userJson = resUser.result().getJsonArray("docs").getJsonObject(0);
            logger.info("userJson: " +userJson.encodePrettily());
            LocalDateTime requestTimePlusInterval = LocalDateTime.parse(userJson.getString("newPasswordRequested").substring(0, 19), formatter).plusDays(1);
			logger.info("requestTime: " + requestTimePlusInterval.toString());
			if (requestTimePlusInterval.isBefore(LocalDateTime.now(Clock.systemUTC()))) {
				handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",403).put("message", "ID Expired")));
				return;				
			}
			sm.getOrientPersistorService().find("passwordParams", new JsonObject().put("username", userJson.getString("username").toUpperCase()), ppRes -> {
				JsonObject passwordParams = UsersResourceManager.generateHashedPassword(password,config.getInteger("passwordHashIterations"));
				sm.getOrientPersistorService().update(ppRes.result().getJsonArray("docs").getJsonObject(0).getString("@rid"), passwordParams, updateRes -> {
					logger.info("updateRes: " + updateRes.result().encodePrettily());
					handler.handle(Future.succeededFuture(updateRes.result()));
				});
			});
		});
	}
	public MailMessage getForgotPasswordMail(JsonObject user, String newPasswordID) {
		logger.info("sendForgotPasswordMail. user:" + user.encodePrettily());
		MailMessage email = new MailMessage();
		email.setFrom(config.getString("siteEmail"));	      				
		email.setTo(user.getString("email"));
		List<String> ccList = new ArrayList<String>();
		for (int i = 0; i < config.getJsonArray("adminEmails").size(); i++) {
			ccList.add(config.getJsonArray("adminEmails").getString(i));
		}
		email.setCc(ccList);
		email.setSubject("בקשת סיסמה חדשה");
		String body = new String(new byte[0], Charset.forName("UTF-8"));
		body += "<html lang='HE'>";
		body += "<body style='text-align:right; direction:rtl;'>";
		body += "באפליקציית " + config.getString("appName") + " נעשתה בקשה לחידוש סיסמה לחשבון עם הפרטים הבאים:<br>";
		body += "מספר טלפון: " + user.getString("username") + "<br>";
		body += "אימייל: " + user.getString("email") + "<br>";
		body += "אם הבקשה נשלחה בטעות, פשוט התעלם מהמייל הזה.<br>";
		body += "לעידכון הסיסמה אנא עבור לקישור הבא:<br>";
		body += config.getString("newPasswordPath") + "?id=" + newPasswordID + "<br>";
		body += "בברכה <br>";
		body += "צוות " + config.getString("appName") + "<br>";
		body += "</body></html>";
		email.setHtml(body);
		logger.trace("mail:" + email.toJson().encodePrettily());
		return email;
	}
	@Override
	public void forgotPassword(JsonObject params, AsyncResultHandler<JsonObject> handler) {
		logger.info("forgotPassword. params: " + params.encodePrettily());	
		sm.getOrientPersistorService().find("users", new JsonObject().put("username", params.getString("phone")), resFind -> {
			if (resFind.result().getJsonArray("docs").size() == 0)
				handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",403).put("message", "no such user")));
			JsonObject user = resFind.result().getJsonArray("docs").getJsonObject(0);
			String newPasswordID = UUID.randomUUID().toString();
			LocalDateTime nowInUtc = LocalDateTime.now(Clock.systemUTC());
			sm.getOrientPersistorService().update(user.getString("@rid"), new JsonObject().put("newPasswordID", newPasswordID).
					put("newPasswordRequested",nowInUtc.format(DateTimeFormatter.ISO_DATE_TIME)), resUpdate -> {
				MailMessage email  = this.getForgotPasswordMail(user, newPasswordID);
				sm.getMailerService().sendMail(email, resMailer -> {
					logger.trace("mailer: " + resMailer.result());
					if (resMailer.succeeded())
						handler.handle(Future.succeededFuture(new JsonObject().put("status", "ok")));
					else
						handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("statusCode",500).put("message", "problem sending mail")));
				});
			});
		});
	}
}
