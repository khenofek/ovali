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

import java.util.HashMap;
import java.util.Map;

import com.leancrowds.ovali.orient.OrientPersistorInterface;
import com.leancrowds.ovali.orient.OrientPersistorService;
import com.leancrowds.ovali.rest.RestManagerInterface;
import com.leancrowds.ovali.rest.RestManagerService;
import com.leancrowds.ovali.users.UsersManagerInterface;
import com.leancrowds.ovali.users.UsersManagerService;

import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author khen
 * Defines services routing (including services/updateServicesConfig endpoint + services/<service type> endpoints)
 * Constructs OvaliServices implementations and delegators
 */
public class OvaliServiceManager extends OvaliConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(OvaliServiceManager.class);
	protected Map<String,OvaliServiceInterface> servicesByType = new HashMap<String,OvaliServiceInterface>();
	UsersManagerService umService;
	OrientPersistorService opService;
	RestManagerService restService;
	OvaliMailService mailerService;
	
	public String getBaseURL() {
		if (config.getJsonObject("http-server").getString("baseURL") != null)
			return config.getJsonObject("http-server").getString("baseURL");
		else
			return "http://" + config.getJsonObject("http-server").getString("host") + ":" + config.getJsonObject("http-server").getString("port");
	}	
	public class ServicesIniter implements AsyncResultHandler<JsonObject> {
		Map<String, OvaliServiceInterface> services;
		HashMap<String,Boolean> results = new HashMap<String,Boolean>();
		AsyncResultHandler<HashMap<String,Boolean>> myDoneHandler = null;
		OvaliServiceInterface currentService = null;
		
		public ServicesIniter(Map<String, OvaliServiceInterface> services) { 
			this.services = services;
		}
		public ServicesIniter doServicesInit(AsyncResultHandler<HashMap<String,Boolean>> handler) {
			myDoneHandler = handler;
			for (Map.Entry<String,OvaliServiceInterface> serviceEntry: services.entrySet()) {
				currentService = serviceEntry.getValue();
				serviceEntry.getValue().serviceInit(this);
			}
			return this;
		}
		@Override
		public void handle(AsyncResult<JsonObject> res) {
			results.put(currentService.getServiceType(), res.succeeded());
			if (results.size() == services.size()) {
				myDoneHandler.handle(Future.succeededFuture(results));
			}
		}
	}
	@Override
	public void start(Future<Void> startFuture) throws Exception {
		logger.trace("start");
		super.startConfigurator(resConfigurator -> {
			logger.info("Configurator start success: " + resConfigurator.succeeded());
			servicesInit(resServices -> {
				if (resServices.succeeded()) {
			        startFuture.complete();
			    } else {
			        startFuture.fail("Configurator failed");
			    }
			});
		});
	}	
	public void servicesInit(AsyncResultHandler<Void> handler) {
		logger.trace("servicesInit");
		instantiateOvaliServices();		
		new ServicesIniter(servicesByType).doServicesInit(res -> {
			getRouter().route().handler(StaticHandler.create().setFilesReadOnly(config.getJsonObject("http-server").getBoolean("filesReadOnly")).
					setCachingEnabled(config.getJsonObject("http-server").getBoolean("caching")));
			logger.info("Services init result: " + res.result().toString());
			handler.handle(Future.succeededFuture());
		});		
	}
	protected void instantiateOvaliServices() {
		umService = new UsersManagerService(this,config.getJsonObject("services").getJsonObject("users-manager"));
		servicesByType.put(umService.getServiceType(), umService);
		opService = new OrientPersistorService(this,config.getJsonObject("services").getJsonObject("orient-persistor"));
		servicesByType.put(opService.getServiceType(), opService);
		restService = new RestManagerService(this,config.getJsonObject("services").getJsonObject("rest-manager"));
		servicesByType.put(restService.getServiceType(), restService);
		mailerService = new OvaliMailService(this,config.getJsonObject("services").getJsonObject("mailer"));
		servicesByType.put(mailerService.getServiceType(), mailerService);
	}
	
	public UsersManagerInterface getUsersManagerService() {
		return umService.getConcreteService("1");
	}
	public OrientPersistorInterface getOrientPersistorService() {
		return opService.getConcreteService("1");
	}
	public OrientPersistorInterface getOrientPersistorService(String containerVersion) {
		// get service version from containerVersion
		return opService.getConcreteService("2");
	}	
	public RestManagerInterface getRestManagerService() {
		return restService.getConcreteService("1");
	}
	public OvaliMailInterface getMailerService() {
		return mailerService.getConcreteService("1");
	}	
}
