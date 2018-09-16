
package cc.cmu.edu.mysql;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletException;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import io.undertow.Handlers;
import io.undertow.Undertow;

/*
 * This is the Front-end main entry
 * It receives and routes traffic according to servlet mapping
 */
public class MysqlUndertowFrontend {

	public MysqlUndertowFrontend() throws Exception {
	}

	public static final String PATH = "/";

	public static void log(String log) {
		System.out.println(log);
	}

	public static void main(String[] args) throws Exception {
		try {
			DeploymentInfo servletBuilder = deployment().setClassLoader(MysqlUndertowFrontend.class.getClassLoader())
					.setContextPath(PATH).setDeploymentName("handler.war")
					.addServlets(servlet("Q1Servlet", Q1Servlet.class).addMapping("/q1"))
					.addServlets(servlet("Q2MysqlServlet", Q2MysqlServlet.class).addMapping("/q2"))
					.addServlets(servlet("Q3MysqlServlet", Q3MysqlServlet.class).addMapping("/q3"));
			DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
			manager.deploy();
			HttpHandler servletHandler = manager.start();
			PathHandler path = Handlers.path(Handlers.redirect(PATH)).addPrefixPath(PATH, servletHandler);

			int ioThreads = 16;
			int workerThreads = ioThreads * 8;
			log("ioThreads=" +Integer.toString(ioThreads));
			Undertow server = Undertow.builder().addHttpListener(80, "0.0.0.0").setWorkerThreads(workerThreads).setIoThreads(ioThreads)
				.setHandler(path).build();
			server.start();
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}
}
