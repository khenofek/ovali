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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.leancrowds.ovali.OvaliServiceManager;
import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientPersistorReal implements OrientPersistorInterface {
	private static final Logger logger = LoggerFactory.getLogger(OrientPersistorReal.class);
	JsonObject config = null;
	OvaliServiceManager sm;
	protected String dbURL;
	protected String dbUser;
	protected String dbPassword;
	protected OPartitionedDatabasePool orientDBPool = null;
	
	public OrientPersistorReal(OvaliServiceManager sm, JsonObject config) {
		this.config = config;
		this.sm = sm;
		this.dbURL = config.getString("dbURL");
		this.dbUser = config.getString("dbUser");
		this.dbPassword = config.getString("dbPassword");
	}
	@Override
	public JsonObject init() {
		startOrientServer();
		orientDBPool = new OPartitionedDatabasePool(dbURL,dbUser,dbPassword);
		return new JsonObject().put("status", "ok");
	}	
	private void startOrientServer() {
		try {
//			String orientdbHome = new File("").getAbsolutePath(); //Set OrientDB home to current directory
//		    System.setProperty("ORIENTDB_HOME", orientdbHome);
			OServer server = OServerMain.create();			
			logger.info("starting OrientDB sever. Absolute Path: " + new File("").getAbsolutePath());
			if (config.getString("orientDBServerConfigFile") != null) {
				server.startup(new File(config.getString("orientDBServerConfigFile")));
			} else
			server.startup(
			   "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
			   + "<orient-server>"
			   + "<network>"
			   + "<protocols>"
			   + "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
			   + "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
			   + "</protocols>"
			   + "<listeners>"
			   + "<listener ip-address=\"" + config.getString("serverIPaddress") + "\" port-range=\"2424-2430\" protocol=\"binary\"/>"
			   //+ "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
			   //+ "<listener ip-address=\"127.12.136.129\" port-range=\"2424-2430\" protocol=\"binary\"/>"
			   + "<listener ip-address=\"" + config.getString("serverIPaddress") + "\" port-range=\"2480-2490\" protocol=\"http\"/>"
			   //+ "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>"
			   //+ "<listener ip-address=\"127.12.136.129\" port-range=\"2480-2490\" protocol=\"http\"/>"
			   + "</listeners>"
			   + "</network>"
			   + "<users>"
			   + "<user name=\"root\" password=\"mcorient!1\" resources=\"*\"/>"
			   + "</users>"
			   + "<properties>"
			   //+ "<entry name=\"server.database.path\" value=\"/tmp/databases\" />" // Didn't work
			   + "<entry name=\"orientdb.www.path\" value=\"/home/khen/Tools/orientdb-community-1.7.9/www/\"/>"
			   //+ "<entry name=\"orientdb.config.file\" value=\"/home/khen/Tools/releases/orientdb-community-1.7-rc2/config/orientdb-server-config.xml\"/>"
			   + "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
			   + "<entry name=\"log.console.level\" value=\"info\"/>"
			   + "<entry name=\"log.file.level\" value=\"fine\"/>"
			   //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
			   + "<entry name=\"plugin.dynamic\" value=\"false\"/>"
			   + "</properties>" + "</orient-server>");
			server.activate();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class BlockingBase {
		ODatabaseDocumentTx db = null;
		public void setDB(ODatabaseDocumentTx db) { this.db = db; }
		public JsonObject run() { return new JsonObject(); }
	}	
	private void runBlocking(BlockingBase r, Handler<AsyncResult<JsonObject>> res) {
		sm.getVertx().<JsonObject>executeBlocking(future -> {
			ODatabaseDocumentTx db = orientDBPool.acquire();
			r.setDB(db);
			logger.trace("runBlocking: " + r.toString());
			try {
				future.complete(r.run());			
			} catch (OException oe) {
				future.fail(oe);
			} finally {
				db.close();
			} 			
		}, resBlocking -> {
			if (resBlocking.succeeded()) {
				res.handle(Future.succeededFuture(resBlocking.result()));
	        } else {
	        	resBlocking.cause().printStackTrace();
	        	res.handle(Future.failedFuture(resBlocking.cause()));
	        }
		});		
	}
	private class runGet extends BlockingBase {		
		String id;
		public runGet(String id) { this.id = id; }
		public JsonObject run() { return new JsonObject().put("doc",new JsonObject(db.getRecord(new ORecordId("#" + id)).toJSON())).put("status", "ok"); }
	}		
	@Override
	public void get(String id, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runGet(id), res);
	}
	private class runGetall extends BlockingBase {		
		String cls;
		public runGetall(String cls) { this.cls = cls; }
		public JsonObject run() { 
			JsonArray replyJsonArray = new JsonArray();
			for (ODocument doc : db.browseClass(cls)) {
				replyJsonArray.add(new JsonObject(doc.toJSON()));
			}
			return new JsonObject().put("docs", replyJsonArray).put("status", "ok");	
		}
	}			
	@Override
	public void getall(String cls, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runGetall(cls), res);
	}
	@Override
	public void find(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runCmd(OrientPersistorJsonBuilder.buildQuery(cls, properties, false)), res);
	}
	@Override
	public void count(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runCmd(OrientPersistorJsonBuilder.buildQuery(cls, properties, true)), res);
	}
	private class runUpdate extends BlockingBase {		
		String id; JsonObject doc;
		public runUpdate(String id, JsonObject doc) { this.id = id; this.doc = doc; }
		public JsonObject run() { 
			ODocument origDoc = db.getRecord(new ORecordId("#" + id));
			ODocument newDoc = new ODocument();
			newDoc.fromJSON(doc.toString());
			ODocument updatedDoc = origDoc.merge(newDoc, true, true);
			updatedDoc.save();
			return new JsonObject().put("doc", new JsonObject(updatedDoc.toJSON())).put("status", "ok");	
		}
	}			
	@Override
	public void update(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runUpdate(id, doc), res);
	}
	private class runCreate extends BlockingBase {		
		String cls; JsonObject doc;
		public runCreate(String cls, JsonObject doc) { this.cls = cls; this.doc = doc; }
		public JsonObject run() { 
			ODocument oDoc = new ODocument(cls);
			oDoc.fromJSON(doc.toString()).save();
			return new JsonObject().put("doc", new JsonObject(oDoc.toJSON())).put("status", "ok");	
		}
	}				
	@Override
	public void create(String cls, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runCreate(cls, doc), res);
	}
	private class runOverride extends BlockingBase {		
		String id; JsonObject doc;
		public runOverride(String id, JsonObject doc) { this.id = id; this.doc = doc; }
		public JsonObject run() { 
			ODocument origDoc = db.getRecord(new ORecordId("#" + id));
			origDoc.clear();
			origDoc.fromJSON(doc.toString());
			origDoc.save();
			return new JsonObject().put("doc", new JsonObject(origDoc.toJSON())).put("status", "ok");	
		}
	}			
	@Override
	public void override(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runOverride(id, doc), res);
	}
	private class runDelete extends BlockingBase {		
		String id;
		public runDelete(String id) { this.id = id; }
		public JsonObject run() {
			db.delete(new ORecordId("#" + id));
			return new JsonObject().put("status", "ok");
		}
	}			
	@Override
	public void delete(String id, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runDelete(id), res);
	}
	private class runCmd extends BlockingBase {		
		String command;
		public runCmd(String command) { this.command = command; }
		public JsonObject run() { 
			JsonArray replyJsonArray = new JsonArray();			
			List<ODocument> result = null;
			result = db.query(new OSQLSynchQuery<ODocument>(command));
			for (ODocument doc : result) {
				replyJsonArray.add(new JsonObject(doc.toJSON()));
			}
			return new JsonObject().put("docs", replyJsonArray).put("status", "ok");	
		}
	}
	@Override
	public void cmd(String command, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runCmd(command), res);
	}
	private class runNonIndempotentCmd extends BlockingBase {		
		String command;
		public runNonIndempotentCmd(String command) { this.command = command; }
		public JsonObject run() { 
			JsonArray replyJsonArray = new JsonArray();
			if (command.contains("return after") || command.contains("return before")) {
				List<ODocument> result = null;
				result = db.command(new OCommandSQL(command)).execute();
				for (ODocument doc : result) {
					replyJsonArray.add(new JsonObject(doc.toJSON()));
				}
				return new JsonObject().put("docs", replyJsonArray).put("status", "ok");			
			} else {
				Integer count = db.command(new OCommandSQL(command)).execute();
				return new JsonObject().put("count", count).put("status", "ok");	
			}	
		}
	}	
	@Override
	public void nonIndempotentCmd(String command, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runNonIndempotentCmd(command), res);
	}
	private class runFindone extends BlockingBase {		
		String command;
		public runFindone(String command) { this.command = command; }
		public JsonObject run() { 
			JsonObject replyJson = null;		
			List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(command));
			if (result.size() == 1) replyJson = new JsonObject(result.get(0).toJSON());
			return new JsonObject().put("result", replyJson);	
		}
	}
	
	private BlockingBase getBlockingBase(JsonObject action) {
		logger.trace("action: " + action.encodePrettily());
		switch (action.getString("action")) {
		case "getone":
			return new runGet(action.getString("rid").substring(1));
		case "getall":
			return new runGetall(action.getString("class"));
		case "find":
			return new runCmd(OrientPersistorJsonBuilder.buildQuery(action.getString("class"), action.getJsonObject("properties"), false));
		case "save":
			return new runCreate(action.getString("class"), action.getJsonObject("doc"));
		case "update":
			return new runUpdate(action.getString("rid").substring(1), action.getJsonObject("doc"));
		case "override":
			return new runOverride(action.getString("rid").substring(1), action.getJsonObject("doc"));
		case "delete":
			return new runDelete(action.getString("rid").substring(1));
		case "sqlCommand":
			if (action.getBoolean("non-idempotent") == null || action.getBoolean("non-idempotent").equals(false))
				return new runCmd(action.getString("command"));
			else return new runNonIndempotentCmd(action.getString("command"));
		case "batch":
			return new runBatch(action.getJsonArray("actions"));
		// findoneMongoStyle (To be removed)
		case "findone":
			return new runFindone(OrientPersistorJsonBuilder.buildQuery("passwordParams", new JsonObject().
					put("username", action.getJsonObject("matcher").getString("username").toUpperCase()).
					put("hashedPassword",action.getJsonObject("matcher").getString("password")), false));
        }
		return null;
	}
	@Override
	public void act(JsonObject params, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(getBlockingBase(params), res);
	}
	private class runBatch extends BlockingBase {		
		JsonArray actions = null;
		public runBatch(JsonArray actions) { this.actions = actions; }
		public JsonObject run() {
			JsonArray replyJsonArray = new JsonArray();
			for (int i=0;i<actions.size();++i) {
				BlockingBase rb = getBlockingBase(actions.getJsonObject(i));
				rb.setDB(db);
				replyJsonArray.add(rb.run());			
			}
			return new JsonObject().put("replies", replyJsonArray).put("status", "ok");	
		}
	}
	@Override
	public void doBatch(OrientPersistorBatch batch, Handler<AsyncResult<JsonObject>> res) {
		runBlocking(new runBatch(batch.getActions()), res);
	}
}
