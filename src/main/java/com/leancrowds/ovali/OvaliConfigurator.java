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

import java.util.Iterator;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class OvaliConfigurator extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(OvaliConfigurator.class);
	protected JsonObject config = null;
	private Router router = null;
	
	public Router getRouter() {
		return router;
	}
	public void setRouter(Router router) {
		this.router = router;
	}
	public void routeAndListen() {
	    setRouter(Router.router(vertx));
//	    getRouter().route("/").handler(routingContext -> {
//	      routingContext.response().putHeader("content-type", "text/html").end("");
//	    });
	    JsonObject httpConfig = config.getJsonObject("http-server");
	    vertx.createHttpServer().requestHandler(getRouter()::accept).listen(httpConfig.getInteger("port", 8080),httpConfig.getString("host","localhost"));
	    logger.info("OvaliConfigurator started with config:" + config.encodePrettily());
	}
	public static void recursiveMergeIn(JsonObject base, JsonObject other) {
		logger.trace("base: " + base.encodePrettily() + ", other: " + other.encodePrettily());
    	Iterator<Map.Entry<String,Object>> baseIter = base.iterator();
        while (baseIter.hasNext()){
          Map.Entry<String,Object> baseEntry = baseIter.next();
          if (base.getBoolean("recurseMergeIn") != null && base.getBoolean("recurseMergeIn").equals(true) && other.getJsonObject(baseEntry.getKey()) != null) {
	          JsonObject baseValue = (JsonObject) baseEntry.getValue();	    
	          if (baseValue.getBoolean("recurseMergeIn") != null && baseValue.getBoolean("recurseMergeIn").equals(true))
	        	  recursiveMergeIn(baseValue, other.getJsonObject(baseEntry.getKey()));
	          else baseValue.mergeIn(other.getJsonObject(baseEntry.getKey()));
          }
        }				    		
    	Iterator<Map.Entry<String,Object>> otherIter = other.iterator();
        while (otherIter.hasNext()){
          Map.Entry<String,Object> otherEntry = otherIter.next();
          if (base.getJsonObject(otherEntry.getKey()) == null) base.put(otherEntry.getKey(), otherEntry.getValue());
        }
	}
	public void startConfigurator(AsyncResultHandler<Void> handler) {
		this.config = config();
		final String ovaliDeployConf = System.getenv("OVALI_DEPLOY_CONF");
		if (ovaliDeployConf != null) {
			logger.info("Deployment configuration override file: " + ovaliDeployConf);
			vertx.fileSystem().readFile(ovaliDeployConf, result -> {
			    if (result.succeeded()) {
			    	JsonObject overrideJson = new JsonObject(result.result().toString());
			    	recursiveMergeIn(config, overrideJson);
			        routeAndListen();				        
				    handler.handle(Future.succeededFuture());
			    } else {
			        logger.error("Error while loading Deployment configuration override file: " + result.cause());
			        handler.handle(Future.failedFuture("Error while loading Deployment configuration override file"));
			    }
			});
		} else {
			routeAndListen();			
			handler.handle(Future.succeededFuture());
		}
	}
}
