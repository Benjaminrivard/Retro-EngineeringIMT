/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: TagXmlAction.java,v 1.2 2007/01/17 18:00:06 basler Exp $ */

package com.sun.javaee.blueprints.petstore.controller.actions;

import com.sun.javaee.blueprints.petstore.controller.ControllerAction;
import com.sun.javaee.blueprints.petstore.model.CatalogFacade;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This action class serves up XML needed for user-created Tags.
 * @author Mark Basler
 * @author Inderjeet Singh
 */
public class TagXmlAction implements ControllerAction {
    
    private final CatalogFacade cf;
    public TagXmlAction(CatalogFacade cf) {
        this.cf = cf;
    }
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out=response.getWriter();
        String itemId=request.getParameter("itemId");
        String sxTags=request.getParameter("tags");
        //if(bDebug) System.out.println("Have tagServlet " + itemId + " - " + sxTags);
        try {
            cf.addTagsToItemId(sxTags, itemId);
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        out.println("<response>");
        out.print("<itemId>");
        out.print(itemId);
        out.println("</itemId>");
        out.print("<tags>");
        out.print(cf.getItem(itemId).tagsAsString());
        out.println("</tags>");
        out.println("</response>");
        out.close();
    }
}
