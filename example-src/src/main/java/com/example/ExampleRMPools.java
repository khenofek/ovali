package com.example;

import io.vertx.core.json.JsonObject;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.rest.ResourceManagerInterface;
import com.leancrowds.ovali.rest.ResourceManagersPools;

public class ExampleRMPools extends ResourceManagersPools {

	public ExampleRMPools(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
	}
	@Override
	public ResourceManagerInterface create(String resource) throws Exception {
		switch (resource) {
			case "contacts":
				return new ContactsResourceManager(resource,sm,config.getJsonObject(resource));

			default:
				return super.create(resource);
		}
	}
}
