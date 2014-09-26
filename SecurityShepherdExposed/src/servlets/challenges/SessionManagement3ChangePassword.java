package servlets.challenges;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;

import utils.ShepherdExposedLogManager;
import dbProcs.Database;

/**
 * Session Management Challenge Three - Change Password
 * This is a level function - DOES NOT RETURN KEY
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class SessionManagement3ChangePassword extends HttpServlet
{

	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(SessionManagement3ChangePassword.class);
	private static String levelName = "Session Management Challenge Three (Change Password)";
	private static String levelHash = "b467dbe3cd61babc0ec599fd0c67e359e6fe04e8cdc618d537808cbb693fee8a";
	private static String levelResult = ""; //This Servlet does not return a result
	/**
	 * Function used by Session Management Challenge Three to change the password of the submitted user name specified in the "Current" cookie
	 * @param current User cookie used to store the current user (encoded twice with base64)
	 * @param newPassword the password which to use to update an accounts password
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdExposedLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		//Attempting to recover username of session that made request
		try
		{
			if (request.getSession() != null)
			{
				HttpSession ses = request.getSession();
				String userName = (String) ses.getAttribute("decyrptedUserName");
				log.debug(userName + " accessed " + levelName + " Servlet");
			}
		}
		catch (Exception e)
		{
			log.debug(levelName + " Servlet Accessed");
			log.error("Could not retrieve user name from session");
		}
		PrintWriter out = response.getWriter();
		Base64 base64 = new Base64();
		out.print(getServletInfo());
		String htmlOutput = new String();
		log.debug(levelName + " - Change Password - Servlet");
		try
		{
			log.debug("Getting Challenge Parameters");
			Cookie userCookies[] = request.getCookies();
			int i = 0;
			Cookie theCookie = null;
			for(i = 0; i < userCookies.length; i++)
			{
				if(userCookies[i].getName().compareTo("current") == 0)
				{
					theCookie = userCookies[i];
					break; //End Loop, because we found the token
				}
			}
			Object passNewObj = request.getParameter("newPassword");
			String subName = new String();
			String subNewPass = new String();
			if(theCookie != null)
				subName = theCookie.getValue();
			if(passNewObj != null)
				subNewPass = (String) passNewObj;
			log.debug("subName = " + subName);
			//Base 64 Decode
			try
			{
				byte[] decodedName = Base64.decodeBase64(subName);
				subName = new String(decodedName, "UTF-8");
				decodedName = Base64.decodeBase64(subName);
				subName = new String(decodedName, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				log.debug("Could not decode username");
				subName = new String();
			}
			log.debug("subName Decoded = " + subName);
			log.debug("subPass = " + subNewPass);
			
			if(subNewPass.length() >= 6)
			{
				log.debug("Getting ApplicationRoot");
				String ApplicationRoot = getServletContext().getRealPath("");
				log.debug("Servlet root = " + ApplicationRoot );
				
				Connection conn = Database.getChallengeConnection(ApplicationRoot, "BrokenAuthAndSessMangChalThree");
				log.debug("Changing password for user: " + subName);
				log.debug("Changing password to: " + subNewPass);
				PreparedStatement callstmt;
				
				callstmt = conn.prepareStatement("UPDATE users SET userPassword = SHA(?) WHERE userName = ?");
				callstmt.setString(1, subNewPass);
				callstmt.setString(2, subName);
				log.debug("Executing changePassword");
				callstmt.execute();
				
				log.debug("Committing changes made to database");
				callstmt = conn.prepareStatement("COMMIT");
				callstmt.execute();
				log.debug("Changes committed.");
				
				htmlOutput = "<p>Password change request success.</p>";
			}
			else
			{
				log.debug("invalid password submitted: " + subNewPass);
				htmlOutput = "<p>Change Password Failed.</p>";
			}
			log.debug("Outputting HTML");
			out.write(htmlOutput);
		}
		catch(Exception e)
		{
			out.write("An Error Occurred! You must be getting funky!");
			log.fatal(levelName + " - Change Password - " + e.toString());
		}
	}
	
}
