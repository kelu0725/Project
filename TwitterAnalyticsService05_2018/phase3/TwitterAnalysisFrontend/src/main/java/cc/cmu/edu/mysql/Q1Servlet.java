package cc.cmu.edu.mysql;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cc.cmu.edu.decode.Decoder;
import cc.cmu.edu.encoder.Encoder;

public class Q1Servlet extends HttpServlet{

	public Q1Servlet() {

	}
	  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
	            throws ServletException, IOException {
		  String type = request.getParameter("type");
		  String data = request.getParameter("data");
		  String result = "";
		  if("encode".equals(type)) {
			  Encoder encoder = new Encoder();
			  result= encoder.encode(data);
		  } else if("decode".equals(type)) {
			  Decoder decoder = new Decoder();
			  result= decoder.decode(data); 
		  }
		  PrintWriter writer = response.getWriter();
          writer.write(result);
          writer.close();
}
	  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
	            throws ServletException, IOException {
	        doGet(request, response);
	    }
	}
