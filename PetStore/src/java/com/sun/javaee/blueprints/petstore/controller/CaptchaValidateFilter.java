/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: CaptchaValidateFilter.java,v 1.24 2007/01/17 18:00:05 basler Exp $ */

package com.sun.javaee.blueprints.petstore.controller;

import com.sun.javaee.blueprints.components.ui.fileupload.FileUploadStatus;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// for CAPTCHA_KEY and CAPTCHA_STRING
import static com.sun.javaee.blueprints.petstore.controller.actions.CaptchaAction.*;
import com.sun.javaee.blueprints.petstore.util.PetstoreUtil;

public class CaptchaValidateFilter implements Filter {
    
    private static final boolean debug = false;
    private static final String CAPTCHA_FIELD_NAME = "j_captcha_response";
    private static final String INVALID_CAPTCHA = "captchaInvalid";
    
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;
    
    /**
     * @return boolean true if captcha is correct
     */
    private Boolean isCaptchaCorrect(HttpServletRequest request, HttpServletResponse response) {
        
        String captchaString = null;
        // Using Cookie instead of actual form body
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(CAPTCHA_FIELD_NAME)) {
                captchaString = cookie.getValue();
            }
        }
        Boolean validResponse = Boolean.FALSE;
        if (captchaString == null) {
            return validResponse;
        }
        // validation can be done within this method - no need to call captcha class
        HttpSession session = request.getSession();
        String storedString = (String) session.getAttribute(CAPTCHA_STRING);
        if (storedString != null && storedString.toLowerCase().equals(captchaString.toLowerCase())) {
            validResponse = Boolean.TRUE;
        }
        return validResponse;
    }
    
    
    /**
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        
        if (debug) log("CaptchaValidateFilter:doFilter()");
        HttpServletRequest httpRequest=(HttpServletRequest)request;
        HttpServletResponse httpReponse=(HttpServletResponse)response;
        Boolean correctCaptcha = isCaptchaCorrect((HttpServletRequest)request, httpReponse);
        
        Throwable problem = null;
        if (correctCaptcha) {
            // check to make sure size of upload isn't too big, over 150kb
            if(request.getContentLength() > 150000) {
                FileUploadStatus fuStatus = new FileUploadStatus();
                httpRequest.getSession(true).setAttribute("fileUploadStatus", fuStatus);
                fuStatus.setMessage("Upload Size Error");
                response.setContentType("text/plain");
                httpReponse.setHeader("Cache-Control", "no-store");
                httpReponse.setHeader("Pragma", "no-cache");
                return;
            }
            
            // if there's previous set session attribute, remove it
            HttpSession session = ((HttpServletRequest)request).getSession(true);
            session.removeAttribute(INVALID_CAPTCHA);
            try {
                chain.doFilter(request, response);
            } catch(Throwable t) {
                //
                // If an exception is thrown somewhere down the filter chain,
                // we still want to execute our after processing, and then
                // rethrow the problem after that.
                //
                problem = t;
                t.printStackTrace();
            }
            
            // possible "after-do" process here
            
            //
            // If there was a problem, we want to rethrow it if it is
            // a known type, otherwise log it.
            //
            if (problem != null) {
                if (problem instanceof ServletException) throw (ServletException)problem;
                if (problem instanceof IOException) throw (IOException)problem;
                sendProcessingError(problem, response);
            }
        } else {
            /* As there's a dojo iframeIO bug for setting header, http response status,
             * it needs to set the "captcha invalid" status to the session attribute
             * for the next request
             */
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(INVALID_CAPTCHA, Boolean.TRUE);
            FileUploadStatus fuStatus = new FileUploadStatus();
            session.setAttribute("fileUploadStatus", fuStatus);
            fuStatus.setMessage("Captchas Filter Error");
            response.setContentType("text/plain");
            httpReponse.setHeader("Cache-Control", "no-store");
            httpReponse.setHeader("Pragma", "no-cache");
        }
    }
    
    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }
    
    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        
        this.filterConfig = filterConfig;
    }
    
    public void destroy() {
    }
    
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (debug && filterConfig != null) {
            log("CaptchaValidateFilter:Initializing filter");
        }
    }
    
    public String toString() {        
        if (filterConfig == null) return ("CaptchaValidateFilter()");
        StringBuffer sb = new StringBuffer("CaptchaValidateFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
        
    }
    
    private void sendProcessingError(Throwable t, ServletResponse response) {
        
        String stackTrace = getStackTrace(t);        
        if(stackTrace != null && !stackTrace.equals("")) {            
            try {                
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n"); //NOI18N
                
                // PENDING! Localize this for next official release
                pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
                pw.print(stackTrace);
                pw.print("</pre></body>\n</html>"); //NOI18N
                PetstoreUtil.closeIgnoringException(pw);
                PetstoreUtil.closeIgnoringException(ps);
                PetstoreUtil.closeIgnoringException(response.getOutputStream());
            } catch(IOException ex){ }
        } else {
            try {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                PetstoreUtil.closeIgnoringException(ps);
                PetstoreUtil.closeIgnoringException(response.getOutputStream());
            } catch(IOException ex){ }
        }
    }
    
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        PetstoreUtil.closeIgnoringException(pw);
        PetstoreUtil.closeIgnoringException(sw);
        return sw.getBuffer().toString();
    }
    
    public void log(String msg) {
        filterConfig.getServletContext().log(msg);
    }
}
