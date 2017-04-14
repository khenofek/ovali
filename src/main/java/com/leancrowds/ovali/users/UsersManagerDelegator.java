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

import com.leancrowds.ovali.OvaliServiceManager;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UsersManagerDelegator implements UsersManagerInterface {
	private static final Logger logger = LoggerFactory.getLogger(UsersManagerDelegator.class);
	JsonObject config = null;
	String delegationHost = null;
	Integer delegationPort = null;
	OvaliServiceManager sm;

	public UsersManagerDelegator(OvaliServiceManager sm, JsonObject config) {
		logger.info("UsersManagerDelegator constructor. config = " + config.encodePrettily());
		this.config = config;
		delegationHost = config.getString("delegationHost");
		if (config.getInteger("delegationPort") != null) delegationPort = config.getInteger("delegationPort");
		this.sm = sm;
	}

	@Override
	public void login(JsonObject credentials,AsyncResultHandler<JsonObject> handler) {
		logger.info("Got credentials: " + credentials);
		HttpClientOptions options = new HttpClientOptions().setDefaultHost(delegationHost);
		if (delegationPort != null) options.setDefaultPort(delegationPort);
		options.setSsl(config.getBoolean("ssl"));
		HttpClient client = sm.getVertx().createHttpClient(options);
		HttpClientRequest request = client.post("/users/login", response -> {
			logger.info("Received response with status code " + response.statusCode());
			response.bodyHandler(buffer -> {
				logger.info("Received response body: " + buffer);
				handler.handle(Future.succeededFuture(buffer.toJsonObject()));
			});			  
		});
		request.end(credentials.toString());
	}

	@Override
	public void authenticateLoginSessionID(String sessionID,
			AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void register(JsonObject userData, JsonObject options, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout(String loginSessionID, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void whosOnline(AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkUUID(String uuid, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newPassword(String newPasswordToken, String password, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forgotPassword(JsonObject params, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activate(String registrationID, AsyncResultHandler<JsonObject> handler) {
		// TODO Auto-generated method stub
		
	}

}
