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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OvaliGenericDelegator implements OvaliGenericInterface {
	private static final Logger logger = LoggerFactory.getLogger(OvaliGenericDelegator.class);
	String endpoint = null;
	JsonObject config = null;
	String delegationHost = null;
	Integer delegationPort = null;
	boolean ssl = false;
	boolean trustAll = false;
	OvaliServiceManager sm;

	public OvaliGenericDelegator(OvaliServiceManager sm, JsonObject config) {
		this.config = config;
		if (config != null) { 
			logger.trace("OvaliGenericDelegator constructor. config = " + config.encodePrettily());
			endpoint = config.getString("endpoint");
			delegationHost = config.getString("delegationHost");
			if (config.getInteger("delegationPort") != null) delegationPort = config.getInteger("delegationPort");
			if (config.getBoolean("useTLS") != null) ssl = config.getBoolean("useTLS");
			if (config.getBoolean("trustAll") != null) trustAll = config.getBoolean("trustAll");
		}
		this.sm = sm;
	}

	@Override
	public void act(JsonObject params, Handler<AsyncResult<JsonObject>> res) {
		logger.info("Got params: " + params.encodePrettily());
		HttpClientOptions options = new HttpClientOptions().setDefaultHost(delegationHost);
		if (delegationPort != null) options.setDefaultPort(delegationPort);
		options.setSsl(ssl);
		options.setTrustAll(trustAll);
		HttpClient client = sm.getVertx().createHttpClient(options);
		HttpClientRequest request = client.post(endpoint, response -> {
			logger.info("Received response with status code " + response.statusCode());
			response.bodyHandler(buffer -> {
				logger.trace("Received response body: " + buffer);
				res.handle(Future.succeededFuture(buffer.toJsonObject()));
			});			  
		});
		request.exceptionHandler(e -> {
			  logger.error("Received exception: " + e.getMessage());
			  e.printStackTrace();
			  res.handle(Future.failedFuture(e));
		});
		request.putHeader("OVALI_SIGNATURE", "35AC678DF");
		request.end(params.toString());
	}

}
