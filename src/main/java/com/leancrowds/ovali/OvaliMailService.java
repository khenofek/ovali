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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.StartTLSOptions;

public class OvaliMailService extends OvaliAbstractService<OvaliMailInterface> implements OvaliMailInterface {
	private static final Logger logger = LoggerFactory.getLogger(OvaliMailService.class);
	MailClient mailClient;
	
	public OvaliMailService(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
	}
	@Override
	public String getServiceType() {
		return "mailer";
	}
	@Override
	public void updateConfig(JsonObject config) {
		logger.trace("config: " + config);
		this.config = config;
		this.concreteService = this;
		if (config.getBoolean("fake") == true) mailClient = null;
		else {
			MailConfig mailConf = new MailConfig();
			mailConf.setHostname(config.getString("hostname"));
			mailConf.setPort(config.getInteger("port"));		
			mailConf.setUsername(config.getString("username"));
			mailConf.setPassword(config.getString("password"));
			mailConf.setSsl(config.getBoolean("ssl"));
			if (config.getString("TLS") != null && config.getString("TLS").equals("DISABLED")) mailConf.setStarttls(StartTLSOptions.DISABLED);
			mailClient = MailClient.createShared(sm.getVertx(), mailConf);
		}
	}

	@Override
	public void sendMail(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
		logger.trace("email: " + email + ", mailClient: " + mailClient);
		if (mailClient != null) {
			mailClient.sendMail(email, result -> {
			  if (result.succeeded()) {
			    logger.info(result.result());
			    resultHandler.handle(Future.succeededFuture(result.result()));
			  } else {
			    result.cause().printStackTrace();
			    resultHandler.handle(Future.failedFuture("mailer failed"));
			  }
			});
		} else resultHandler.handle(Future.succeededFuture(new MailResult()));
	}
	@Override
	public String getSiteMail() {
		return config.getString("sitemail");
	}
}
