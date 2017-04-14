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

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class OvaliJsonValidator {
	private static final Logger logger = LoggerFactory.getLogger(OvaliJsonValidator.class);
	Schema schema;
	Vertx vertx;
	
	public OvaliJsonValidator(String jsonSchema, Vertx vertx) {
		JSONObject rawSchema = new JSONObject(jsonSchema);
		schema = SchemaLoader.load(rawSchema);
		this.vertx = vertx;
	}
	public void validate(String jsonStringToValidate,AsyncResultHandler<JsonObject> handler) {
		vertx.executeBlocking(future -> {
			JSONObject jsonToValidate = new JSONObject(jsonStringToValidate);
			try {
				  schema.validate(jsonToValidate);
				} catch (ValidationException e) {
				  System.out.println(e.getMessage());
				  e.getCausingExceptions().stream()
				      .map(ValidationException::getMessage)
				      .forEach(System.out::println);
				  future.fail(e);
				  return;
				}		
			  future.complete();
			}, res -> {
				if (res.succeeded()) {
					logger.trace("The result is: " + res.result());
					handler.handle(Future.succeededFuture());
				} else handler.handle(Future.failedFuture(res.cause()));
			});
	}
}
