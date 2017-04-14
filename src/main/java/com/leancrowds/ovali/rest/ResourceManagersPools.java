package com.leancrowds.ovali.rest;

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

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.users.UsersResourceManager;

public class ResourceManagersPools extends BaseKeyedPooledObjectFactory<String, ResourceManagerInterface> {
	private static final Logger logger = LoggerFactory.getLogger(ResourceManagersPools.class);
	protected JsonObject config = null;
	protected OvaliServiceManager sm;

	public ResourceManagersPools(OvaliServiceManager sm, JsonObject config) {
		logger.info("config: " + config);
		this.config = config;
		this.sm = sm;
	}
	@Override
	public ResourceManagerInterface create(String resource) throws Exception {
		logger.trace("create resource manager: " + resource);
		switch (resource) {
			case "users":
				return new UsersResourceManager(sm,config.getJsonObject(resource));
			case "events":
				return new PersistentResourceManager(resource,sm,config.getJsonObject(resource));
				
			default:
				return null;
		}
	}

	@Override
	public PooledObject<ResourceManagerInterface> wrap(ResourceManagerInterface rm) {
		return new DefaultPooledObject<ResourceManagerInterface>(rm);
	}
}
