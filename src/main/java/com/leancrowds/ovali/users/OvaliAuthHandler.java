package com.leancrowds.ovali.users;

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

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

public class OvaliAuthHandler implements Handler<RoutingContext> {
	UsersManagerInterface um = null;
	
	public OvaliAuthHandler(UsersManagerInterface usersManagerInterface) {
		this.um = usersManagerInterface;
	}

	@Override
	public void handle(RoutingContext context) {
		if (context.request().headers().get("mc_authorization") != null && !context.request().headers().get("mc_authorization").equals("public")) {
			String auth = context.request().headers().get("mc_authorization");
			String splitAuth[] = auth.split(" ");
			String fields[] = splitAuth[0].split(",");
			String authValues[] = splitAuth[1].split(",");
			JsonObject authJson = new JsonObject();
			int i=0;
			for (String field:fields) {
				authJson.put(field, authValues[i]);
				++i;
			}
			um.authenticateLoginSessionID(authJson.getString("sId"), res -> {
				if (res.succeeded()) {
					if (res.result().getString("status") != null && res.result().getString("status").equals("denied")) context.setUser(null);
					else {
						OvaliUser user = new OvaliUser(res.result());
						context.setUser(user);
					}
				} else {
					context.setUser(null);
				}
				context.next();
			});
		} else {
			context.setUser(null);
			context.next();			
		}
	}
}
