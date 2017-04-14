package com.example;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.rest.ResourceManagerInterface;
import com.leancrowds.ovali.rest.RestManagerReal;

public class ExampleRestManager extends RestManagerReal {
	private static final Logger logger = LoggerFactory.getLogger(ExampleRestManager.class);
	
	public ExampleRestManager(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
		logger.info("constructor. config = " + config);
		this.rmPools = new GenericKeyedObjectPool<String, ResourceManagerInterface>(new ExampleRMPools(sm,config.getJsonObject("resources")));
		rmPools.setBlockWhenExhausted(false);
	}
}
