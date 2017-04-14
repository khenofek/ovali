package com.example;

import com.leancrowds.ovali.OvaliServiceManager;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class ExampleServiceManager extends OvaliServiceManager {
	private static final Logger logger = LoggerFactory.getLogger(ExampleServiceManager.class);
	
	public void redirectToHttpsOnOpenshift(RoutingContext routingContext) {
		final String openshiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
		if (openshiftVertxIP != null && routingContext.request().headers().get("X-Forwarded-Proto") != null &&
				!routingContext.request().headers().get("X-Forwarded-Proto").equals("https")) {
			routingContext.response().setStatusCode(302).putHeader("Location", getBaseURL()).end("302 Temporary Redirect");
		} else routingContext.next();
	}

	@Override
	public void servicesInit(AsyncResultHandler<Void> handler) {
		logger.info("servicesInit");
//		getRouter().route("/*").handler(ctx -> { 
//			logger.info(ctx.request().method() + " " + ctx.request().absoluteURI());
//			ctx.next();
//		});
		getRouter().get("/*").handler(this::redirectToHttpsOnOpenshift);
		super.servicesInit(handler);
	}
}
