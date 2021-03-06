/**
 This file is a part of Verge project.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 @Author Dmitrii Zolotov <zolotov@gathe.org>, Tikhon Tagunov <tagunov@gathe.org>
 */
package org.gathe.verge.pdf;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gathe.verge.uno.ODT2PDFConverter;
import redis.clients.jedis.Jedis;
import java.util.Random;

public class Writer extends HttpServlet {

    protected void init(ServletContext context) throws ServletException {
        
        
    }
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected synchronized void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
        Random randomGenerator = new Random();
    	int randomEntry = randomGenerator.nextInt(1000);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Calendar cal = Calendar.getInstance();
        String filename = dateFormat.format(cal.getTime())+randomEntry;
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(filename.getBytes(), 0, filename.length());
        BigInteger i = new BigInteger(1, m.digest());
        String session = String.format("%1$032x", i);
        String templateID = request.getParameter("templateID");
        Properties templates = new Properties();
        templates.load(new FileReader("/templates/documents.properties"));
//                for (Iterator<Object> it = templates.keySet().iterator(); it.hasNext();) {
//                    String templateKey = ""+it.next();
//                    System.out.println(templateKey);
//                }
        if (templates.containsKey(templateID)) {
            String templateName = ""+templates.get(templateID);
            String parameters = request.getParameter("data");
            if (parameters==null) parameters = "{}";
            String callback = request.getParameter("url");
            if (callback==null) callback = "";
            Thread th = new Thread(new ODT2PDFConverter(templateName, parameters, session, callback));
            th.start();
            if (callback.trim().length()==0) {
        	try {
                	th.join();
                } catch (Exception e) {
                }
            	response.setContentType("application/pdf");
	        OutputStream out2 = response.getOutputStream();
	        try {
	            Jedis jedis = new Jedis("localhost");
	            System.out.println("Extracting from redis");
	            byte[] bytes = jedis.get(session.getBytes("UTF-8"));
	            System.out.println("Length="+bytes.length);
	            out2.write(bytes);
	            out2.flush();
	        } finally {            
	            out2.close();
	        }
            } else {
		    response.setContentType("text/html;charset=UTF-8");
	            PrintWriter out = response.getWriter();
	    	    try {
	              out.println(session);
	    	      out.flush();
	    	    } finally {
	    	      out.close();
	    	    }
            }
        } else {
		    response.setContentType("text/html;charset=UTF-8");
	            PrintWriter out = response.getWriter();
                    out.print("Error: incorrect template name");
                    out.close();
	}
	} catch (NoSuchAlgorithmException e) {};
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Pergamen - report system";
    }// </editor-fold>
}
