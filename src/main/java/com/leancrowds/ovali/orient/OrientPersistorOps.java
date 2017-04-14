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

public interface OrientPersistorOps {
	public void get(String id, Handler<AsyncResult<JsonObject>> res);
	public void getall(String cls, Handler<AsyncResult<JsonObject>> res);
	public void find(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res);
	public void count(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res);
	public void update(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res);
	public void create(String cls, JsonObject doc, Handler<AsyncResult<JsonObject>> res);
	public void override(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res);
	public void delete(String id, Handler<AsyncResult<JsonObject>> res);
	public void cmd(String command, Handler<AsyncResult<JsonObject>> res);
	public void nonIndempotentCmd(String command, Handler<AsyncResult<JsonObject>> res);
}
