/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: ControllerServlet.java,v 1.28 2007/01/17 18:00:06 basler Exp $ */

package com.sun.javaee.blueprints.petstore.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.javaee.blueprints.petstore.controller.actions.CaptchaAction;
import com.sun.javaee.blueprints.petstore.controller.actions.CatalogXmlAction;
import com.sun.javaee.blueprints.petstore.controller.actions.DefaultControllerAction;
import com.sun.javaee.blueprints.petstore.controller.actions.ImageAction;
import com.sun.javaee.blueprints.petstore.controller.actions.TagXmlAction;
import com.sun.javaee.blueprints.petstore.model.CatalogFacade;
import com.sun.javaee.blueprints.petstore.util.PetstoreUtil;

/**
 * This servlet is responsible for interacting with a client
 * based controller and will fetch resources including content
 * and relevant script.
 *
 * This servlet also will process requests for client observers
 */
public class ControllerServlet extends HttpServlet {
    
    private static final boolean bDebug=false;
    private Map<String, ControllerAction> actionMap = new HashMap<String, ControllerAction>();
    
    @Override 
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        CatalogFacade cf = (CatalogFacade) context.getAttribute("CatalogFacade");
        actionMap.put("/ImageServlet", new ImageAction(context));
        actionMap.put("/controller", new DefaultControllerAction(context));
        actionMap.put("/faces/CaptchaServlet", new CaptchaAction());
        actionMap.put("/TagServlet", new TagXmlAction(cf));
        actionMap.put("/catalog", new CatalogXmlAction(cf));
    }
    
    public ControllerAction findAction(String servletPath) {
        return actionMap.get(servletPath);
    }
    @Override 
    public void destroy() {
        actionMap = null;
    }
    
    @Override 
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if(bDebug) System.out.println(" ServletPath: " + servletPath + ", pathinfo: " + request.getPathInfo());
        ControllerAction action = actionMap.get(servletPath);
        if (action != null) {
            if(bDebug) System.out.println(" Found action " + action.getClass().getName());
            action.service(request, response);
        } else {
            PetstoreUtil.getLogger().log(Level.SEVERE, "Servlet '" + request.getServletPath() + "' not registered in ControllerServlet!!");
            HttpServletResponse httpResponse=(HttpServletResponse)response;
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
