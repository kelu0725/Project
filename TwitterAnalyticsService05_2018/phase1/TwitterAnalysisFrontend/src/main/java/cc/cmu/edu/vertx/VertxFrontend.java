package cc.cmu.edu.vertx;

import cc.cmu.edu.decode.Decoder;
import cc.cmu.edu.encoder.Encoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class VertxFrontend extends AbstractVerticle {

	private HttpServer httpServer;
	private Router router;
	private Encoder encoder;
	private Decoder decoder;
	
	@Override
	public void start() throws Exception {
		encoder = new Encoder();
		decoder = new Decoder();
		httpServer = vertx.createHttpServer();
		router = Router.router(vertx);
		router.get("/q1").handler(routingContext -> {
			String type = routingContext.request().getParam("type");
			String data = routingContext.request().getParam("data");
			String result ="";
			  if("encode".equals(type)) {
				  result= encoder.encode(data);
			  }else if("decode".equals(type)) {
				  result= decoder.decode(data);	  
			  }
			HttpServerResponse response = routingContext.response();
			response.setChunked(true);
			response.write(result);
			response.end();
		});
	    httpServer.requestHandler(router::accept).listen(80);
	}
}
