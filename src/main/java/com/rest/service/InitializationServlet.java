package com.rest.service;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.p2p.peercds.cli.ClientWrapper;

public class InitializationServlet extends HttpServlet {
	 
		 
		  private String message;

		  public void init() throws ServletException
		  {
			  System.out.println("Servlet started");
			  ClientWrapper.getClientWrapper();
		      // Do required initialization
		      message = "Hello World";
		  }

		  public void doGet(HttpServletRequest request,
		                    HttpServletResponse response)
		            throws ServletException, IOException
		  {
		      // Set response content type
		      response.setContentType("text/html");

		      // Actual logic goes here.
		      PrintWriter out = response.getWriter();
		      out.println("<h1>" + message + "</h1>");
		  }
		  
		  public void destroy()
		  {
		      // do nothing.
		  }
	

}
