/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: PayPalBean.java,v 1.6 2006/11/02 00:34:49 basler Exp $ */

package com.sun.javaee.blueprints.petstore.model;

import java.math.BigDecimal;
import com.sun.javaee.blueprints.components.ui.shopping.BuyNowPostData;
/**
 *
 * @author basler
 */
public class PayPalBean {
    
    BuyNowPostData postData=null;
   
    /** Creates a new instance of PayPalBean */
    public PayPalBean() {
        postData=new BuyNowPostData();
        postData.setShippingCost(new BigDecimal("25"));
        postData.setTax(new BigDecimal("10"));
        postData.setUndefinedQuantity("1");
        postData.setSubmissionMethod("GET");
    }
    
    public BuyNowPostData getPostData() {
        return postData;
    }
    
    
}
