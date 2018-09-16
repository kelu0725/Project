package cc.cmu.edu.undertow;
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

public class UndertowFrontend{
	public UndertowFrontend() throws Exception{
	}
	public static final String PATH = "/";
	
	public static void main(String[] args) throws Exception{
		try {
		DeploymentInfo servletBuilder = deployment()
				.setClassLoader(UndertowFrontend.class.getClassLoader())
				.setContextPath(PATH)
				.setDeploymentName("handler.war")
				.addServlets(
						servlet("MyServlet", MyServlet.class).addMapping("/q1"));
			DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
			manager.deploy();

			HttpHandler servletHandler = manager.start();
			PathHandler path = Handlers.path(Handlers.redirect(PATH))
					.addPrefixPath(PATH, servletHandler);

			Undertow server = Undertow.builder()
					.addHttpListener(80, "0.0.0.0")
					.setHandler(path)
					.build();
			server.start();
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}
}