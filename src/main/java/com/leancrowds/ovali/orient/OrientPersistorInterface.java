package com.leancrowds.ovali.orient;

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

public interface OrientPersistorInterface extends OrientPersistorOps {
	public JsonObject init();
	public void act(JsonObject params, Handler<AsyncResult<JsonObject>> res);
	
	public void doBatch(OrientPersistorBatch batch, Handler<AsyncResult<JsonObject>> res);
}
