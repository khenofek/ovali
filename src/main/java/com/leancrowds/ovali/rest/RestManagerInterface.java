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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface RestManagerInterface {
	public ResourceManagerInterface getResourceManager(String resource);
	public void act(RoutingContext params, JsonObject restContext, Handler<AsyncResult<JsonObject>> res);
}
