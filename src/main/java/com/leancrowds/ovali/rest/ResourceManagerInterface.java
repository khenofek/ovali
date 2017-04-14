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

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.MultiMap;

public interface ResourceManagerInterface {
	public void isAuthorizedAndValidated(String restAction, JsonObject context, AsyncResultHandler<JsonObject> handler);
	
	public void create(JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler);
	public void read(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler);
	public void query(MultiMap query, JsonObject context, AsyncResultHandler<JsonObject> handler);
	public void update(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler);
	public void override(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler);
	public void delete(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler);	
}
