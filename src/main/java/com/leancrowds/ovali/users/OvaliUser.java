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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

public class OvaliUser extends AbstractUser {
	JsonObject principal = null;
	
	public OvaliUser(JsonObject principal) {
		this.principal = principal;
	}

	@Override
	public JsonObject principal() {		
		return principal;
	}

	@Override
	public void setAuthProvider(AuthProvider arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doIsPermitted(String arg0, Handler<AsyncResult<Boolean>> arg1) {
		// TODO Auto-generated method stub

	}

}
