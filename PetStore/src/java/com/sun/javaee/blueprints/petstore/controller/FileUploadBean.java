/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: FileUploadBean.java,v 1.52 2007/02/24 19:41:08 basler Exp $ */

package com.sun.javaee.blueprints.petstore.controller;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;
import org.apache.shale.remoting.faces.ResponseFactory;
import com.sun.javaee.blueprints.petstore.util.PetstoreUtil;
import com.sun.javaee.blueprints.petstore.util.PetstoreConstants;
import com.sun.javaee.blueprints.petstore.util.ImageScaler;
import com.sun.javaee.blueprints.components.ui.fileupload.FileUploadStatus;
import com.sun.javaee.blueprints.components.ui.fileupload.FileUploadUtil;
import com.sun.javaee.blueprints.petstore.proxy.GeoCoder;
import com.sun.javaee.blueprints.petstore.proxy.GeoPoint;

import com.sun.javaee.blueprints.petstore.model.Address;
import com.sun.javaee.blueprints.petstore.model.CatalogFacade;
import com.sun.javaee.blueprints.petstore.model.Category;
import com.sun.javaee.blueprints.petstore.model.Product;
import com.sun.javaee.blueprints.petstore.model.FileUploadResponse;
import com.sun.javaee.blueprints.petstore.model.Item;
import com.sun.javaee.blueprints.petstore.model.Tag;
import com.sun.javaee.blueprints.petstore.model.SellerContactInfo;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class FileUploadBean {
    private boolean bDebug=true;
    private static final String comma=", ";
    private List<SelectItem> categories = null;
    private List<SelectItem> products = null;
    private CatalogFacade catalogFacade = null;
    
    /**
     * <p>Factory for response writers that we can use to construct the
     * outgoing response.</p>
     */
    private static ResponseFactory factory = new ResponseFactory();
    
    /**
     * session attribute to contain the fileupload status
     */
    private static final String FILE_UL_RESPONSE = "fileuploadResponse";
    
    /** Creates a new instance of FileUploadBean */
    public FileUploadBean() {
    }
    
    public void setProducts(List<SelectItem> products) {
        this.products = products;
    }
    public List<SelectItem> getProducts() {
        if (catalogFacade == null) {
            Map<String,Object> contextMap = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
            this.catalogFacade = (CatalogFacade)contextMap.get("CatalogFacade");
        }
        //get the catalog facade
        if (products == null) {
            products = new ArrayList<SelectItem>();
            for (Product pd : catalogFacade.getProducts()) {
                products.add(new SelectItem(pd.getProductID(), pd.getName()));
            }
        }
        return this.products;
    }
    public void setCategories(List<SelectItem> categories) {
        this.categories = categories;
    }
    
    public List<SelectItem> getCategories() {
        if (catalogFacade == null) {
            Map<String,Object> contextMap = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
            this.catalogFacade = (CatalogFacade)contextMap.get("CatalogFacade");
        }
        //get the catalog facade
        if (categories == null) {
            categories = new ArrayList<SelectItem>();
            for (Category cat : catalogFacade.getCategories()) {
                categories.add(new SelectItem(cat.getCategoryID(), cat.getName()));
            }
        }
        return this.categories;
    }
    
    public void postProcessingMethod(FacesContext context, Hashtable htUpload, FileUploadStatus status) {
        if(bDebug) System.out.println("IN Custom Post Processing method");
        try {
            // set custom return enabled so Phaselistener knows not to send default response
            status.enableCustomReturn();
            HttpServletResponse response=(HttpServletResponse)context.getExternalContext().getResponse();
            
            // get proxy host and port from servlet context
            String proxyHost=context.getExternalContext().getInitParameter("proxyHost");
            String proxyPort=context.getExternalContext().getInitParameter("proxyPort");
            
            // Acquire a response containing these results
            ResponseWriter writer = factory.getResponseWriter(context, "text/xml");
            
            String itemId = null;
            String name = null;
            String thumbPath = null;
            String firstName = null;
            String prodId = null;
            HttpSession session = (HttpSession)context.getExternalContext().getSession(true);
            try {
                String fileNameKey = null;
                for (Object key1 : htUpload.keySet()) {
                    String key = key1.toString();
                    if(key.startsWith("fileLocation_")) {
                        fileNameKey = key;
                        break;
                    }
                }
                if(fileNameKey == null) {
                    // error, you must have and upload file
                    sendErrorResponse(response, writer, PetstoreUtil.getMessage("invalid_item_imageurl"));
                    return;
                }
                
                String absoluteFileName=getStringValue(htUpload, fileNameKey);
                if(bDebug) System.out.println("Abs name: "+ absoluteFileName);
                String fileName = null;
                if(absoluteFileName.length() > 0) {
                    int lastSeparator = absoluteFileName.lastIndexOf("/") + 1;
                    if (lastSeparator != -1) {
                        // set to proper location so image can be read
                        fileName = "images/" + absoluteFileName.substring(lastSeparator, absoluteFileName.length());
                    }
                    String spath = constructThumbnail(absoluteFileName);
                    thumbPath = null;
                    if (spath != null) {
                        // recreate "images/FILENAME"
                        int idx = spath.lastIndexOf("/");
                        thumbPath = "images/"+spath.substring(idx+1, spath.length());
                    }
                } else{
                    fileName = "images/dragon-iron-med.jpg";
                    thumbPath = "images/dragon-iron-thumb.jpg ";
                }
                if(bDebug) System.out.println("*** Final file names - separator: " + fileName + " - " + thumbPath + " - '" + 
                        System.getProperty("file.separator") + "'");
                
                String compName=getStringValue(htUpload, FileUploadUtil.COMPONENT_NAME);
                prodId=getStringValue(htUpload, compName+":product");
                name=getStringValue(htUpload, compName+":name");
                String desc=getStringValue(htUpload, compName+":description");
                String price=getStringValue(htUpload, compName+":price");
                // set to negative to trigger validation message for price
                if(price.length() == 0) price="-1";
                
                String tags=getStringValue(htUpload, compName+":tags");
                if(tags == null) tags="";
                
                //Address
                StringBuffer addressx=new StringBuffer();
                String street1=getStringValue(htUpload, compName+":street1");
                String city=getStringValue(htUpload, compName+":cityField");
                String state=getStringValue(htUpload, compName+":stateField");
                String zip=getStringValue(htUpload, compName+":zipField");
                
                // Contact info
                firstName = getStringValue(htUpload, compName+":firstName");
                String lastName = getStringValue(htUpload, compName+":lastName");
                String email = getStringValue(htUpload, compName+":email");
                
                if(street1 != null && street1.length() > 0) {
                    addressx.append(street1);
                }
                
                if(city != null && city.length() > 0) {
                    addressx.append(comma);
                    addressx.append(city);
                }
                
                if(state != null && state.length() > 0) {
                    addressx.append(comma);
                    addressx.append(state);
                }
                
                if(zip != null && zip.length() > 0) {
                    addressx.append(comma);
                    addressx.append(zip);
                }
                
                // get latitude & longitude
                GeoCoder geoCoder=new GeoCoder();
                if(proxyHost != null && !proxyHost.equals("") && proxyPort != null && proxyPort.equals("")) {
                    // set proxy host and port if it exists
                    // NOTE: This may require write permissions for java.util.PropertyPermission to be granted
                    PetstoreUtil.getLogger().log(Level.INFO, "Setting proxy to " + proxyHost + ":" + proxyPort + ".  Make sure server.policy is updated to allow setting System Properties");
                    geoCoder.setProxyHost(proxyHost);
                    try {
                        geoCoder.setProxyPort(Integer.parseInt(proxyPort));
                    } catch (NumberFormatException ee) {
                        ee.printStackTrace();
                    }
                } else {
                    PetstoreUtil.getLogger().log(Level.INFO, "A \"proxyHost\" and \"proxyPort\" isn't set as a web.xml context-param. A proxy server may be necessary to reach the open internet.");
                }
                
                // use component to get points based on location (this uses Yahoo's map service
                String totAddr=addressx.toString();
                double latitude=0;
                double longitude=0;
                if(totAddr.length() > 0) {
                    try {
                        GeoPoint points[]=geoCoder.geoCode(totAddr);
                        if ((points == null) || (points.length < 1)) {
                            PetstoreUtil.getLogger().log(Level.INFO, "No addresses for location - " + totAddr);
                        } else if(points.length > 1) {
                            PetstoreUtil.getLogger().log(Level.INFO, "Matched " + points.length + " locations, taking the first one");
                        }
                        
                        // grab first address in more that one came back
                        if(points != null && points.length > 0) {
                            // set values to used for map location
                            latitude = points[0].getLatitude();
                            longitude = points[0].getLongitude();
                        }
                    } catch (Exception ee) {
                        PetstoreUtil.getLogger().log(Level.WARNING, "geocoder.lookup.exception", ee);
                    }
                }
                BigDecimal pricex;
                try {
                    pricex=new BigDecimal(price).setScale(2, BigDecimal.ROUND_HALF_UP);
                } catch (NumberFormatException nf) {
                    pricex=BigDecimal.valueOf(-1);
                    PetstoreUtil.getLogger().log(Level.INFO, "Price isn't in a proper number - " + price);
                }
                Address addr = new Address(street1,"",city,state,zip,
                        latitude,longitude);
                SellerContactInfo contactInfo = new SellerContactInfo(firstName, lastName, email);
                Item item = new Item(prodId,name,desc,fileName, thumbPath, pricex,
                        addr,contactInfo,0,0);
                
                // validate item
                String[] errors=item.validateWithMessage();
                if(bDebug) System.out.println("*** validation errors = " + errors.length);
                if(errors.length > 0) {
                    // some validation errors have been hit through an exception
                    StringBuilder sbMess=new StringBuilder();
                    for(String mesg : errors) {
                        sbMess.append(mesg);
                        sbMess.append("\n");
                    }
                    
                    sendErrorResponse(response, writer, sbMess.toString());
                    return;
                }
                
                if (catalogFacade == null) {
                    Map<String,Object> contextMap = context.getExternalContext().getApplicationMap();
                    this.catalogFacade = (CatalogFacade)contextMap.get("CatalogFacade");
                }
                
                // now parse tags for item
                StringTokenizer stTags=new StringTokenizer(tags, " ");
                String tagx=null;
                while(stTags.hasMoreTokens()) {
                    tagx=stTags.nextToken().toLowerCase();
                    Tag tag=null;
                    if(bDebug) System.out.println("Adding TAG = " + tagx);
                    // see if tag is already in item
                    if(!item.containsTag(tagx)) {
                        // add correct tag reference to item
                        item.getTags().add(catalogFacade.addTag(tagx));
                    }
                }
                
                itemId=catalogFacade.addItem(item);
                PetstoreUtil.getLogger().log(Level.FINE, "Item " + name + " has been persisted");
                
            } catch (Exception ex) {
                // since this method is access through an ajax call, must send back a message
                // so the client can forward the user to the systemerror page
                PetstoreUtil.getLogger().log(Level.SEVERE, "fileupload.persist.exception", ex);
                response.sendError(response.SC_INTERNAL_SERVER_ERROR, ex.toString());
                return;
            }
            
            String responseMessage = firstName+", Thank you for submitting your pet "+name+" !";
            FileUploadResponse fuResponse = new FileUploadResponse(
                    itemId,
                    prodId,
                    responseMessage,
                    status.getStatus(),
                    Long.toString(status.getUploadTime()),
                    status.getUploadTimeString(),
                    status.getStartUploadDate().toString(),
                    status.getEndUploadDate().toString(),
                    Long.toString(status.getTotalUploadSize()),
                    thumbPath);
            session.removeAttribute(FILE_UL_RESPONSE);
            session.setAttribute(FILE_UL_RESPONSE, fuResponse);
            /** the following writer operation is for the case when iframe
             * bug is fixed. they are not used currently in the client
             */
            StringBuffer sb=new StringBuffer();
            response.setContentType("text/xml;charset=UTF-8");
            response.setHeader("Pragma", "No-Cache");
            response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
            response.setDateHeader("Expires", 1);
            sb.append("<response>");
            sb.append("<message>");
            sb.append(responseMessage);
            sb.append("</message>");
            sb.append("<status>");
            sb.append(status.getStatus());
            sb.append("</status>");
            sb.append("<duration>");
            sb.append(status.getUploadTime());
            sb.append("</duration>");
            sb.append("<duration_string>");
            sb.append(status.getUploadTimeString());
            sb.append("</duration_string>");
            sb.append("<start_date>");
            sb.append(status.getStartUploadDate());
            sb.append("</start_date>");
            sb.append("<end_date>");
            sb.append(status.getEndUploadDate());
            sb.append("</end_date>");
            sb.append("<upload_size>");
            sb.append(status.getTotalUploadSize());
            sb.append("</upload_size>");
            sb.append("<thumbnail>");
            sb.append(thumbPath);
            sb.append("</thumbnail>");
            sb.append("<itemId>");
            sb.append(itemId);
            sb.append("</itemId>");
            sb.append("<productId>");
            sb.append(prodId);
            sb.append("</productId>");
            sb.append("</response>");
            if(bDebug) System.out.println("Response:\n" + sb);
            writer.write(sb.toString());
            writer.flush();
            
        } catch (IOException iox) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "FileUploadPhaseListener error writting AJAX response", iox);
        }
    }
    
    private String constructThumbnail(String path) {
        String thumbPath = null;
        String aPath=path;
        
        int idx = aPath.lastIndexOf(".");
        if (idx > 0) {
            thumbPath = aPath.substring(0, idx)+"_thumb"+aPath.substring(idx, aPath.length());
        }
        
        try {
            ImageScaler imgScaler = new ImageScaler(aPath);
            imgScaler.keepAspectWithWidth();
            imgScaler.resizeWithGraphics(thumbPath);
        } catch (Exception e) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "ERROR in generating thumbnail", e);
        }
        return thumbPath;
    }
    
    private String getStringValue(Hashtable htUpload, String key)  {
        if(key == null) return null;
        
        String sxTemp=(String)htUpload.get(key);
        if(sxTemp == null) {
            sxTemp="";
        }
        return sxTemp;
    }
    
    public String getUploadImageDirectory() {
        return PetstoreConstants.PETSTORE_IMAGE_DIRECTORY + "/images";
    }
    
    private void sendErrorResponse(HttpServletResponse response, ResponseWriter writer, String mess) throws IOException {
        response.setContentType("text/xml;charset=UTF-8");
        response.setHeader("Pragma", "No-Cache");
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
        response.setDateHeader("Expires", 1);
        writer.write("<response>");
        writer.write("<message>");
        writer.write("Validation Error");
        writer.write("</message>");
        writer.write("<detail>");
        writer.write(mess);
        writer.write("</detail>");
        writer.write("</response>");
    }
    
    
}
