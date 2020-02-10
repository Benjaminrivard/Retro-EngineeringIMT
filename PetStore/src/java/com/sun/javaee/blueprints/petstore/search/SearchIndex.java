/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: SearchIndex.java,v 1.11 2007/01/17 18:00:08 basler Exp $ */

package com.sun.javaee.blueprints.petstore.search;


import java.io.IOException;
import java.util.logging.Level;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.document.Field;

import com.sun.javaee.blueprints.petstore.util.PetstoreUtil;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Base search mechanism for Petstore indexes created from the database data
 * @author basler
 */
public class SearchIndex {
    
    private static final boolean bDebug=false;
    private List<IndexDocument> hitsList=new ArrayList<IndexDocument>();
    private Hits hits=null;
    
    /** Creates a new instance of SearchIndex */
    public SearchIndex() {
    }
    
    public List<IndexDocument> query(String indexFile, String searchString) {
        return query(indexFile, searchString, "contents");
    }
        
    public List<IndexDocument> query(String indexFile, String searchString, String searchField) {
        
        Searcher searcher=null;
        try {
            searcher=new IndexSearcher(indexFile);
            Analyzer analyzer=new StandardAnalyzer();
            // search "contents" attribute by default where all relavant words are kept
            QueryParser queryParser=new QueryParser(searchField, analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.AND);
            Query query=queryParser.parse(searchString);
            
            PetstoreUtil.getLogger().log(Level.INFO, "search.string", searchString);

            // execute search
            hits=searcher.search(query);
            PetstoreUtil.getLogger().log(Level.INFO, "search.results", String.valueOf(hits.length()));
            Document indexDoc;
            Enumeration enumx;
            Field fieldx;
            IndexDocument indexDocument=null;
            for(int ii=0; ii < hits.length(); ii++) {
                indexDoc=hits.doc(ii);
                
                // create new holder for research results
                indexDocument=new IndexDocument();
                
                fieldx=indexDoc.getField("url");
                if(fieldx != null) {
                    indexDocument.setPageURL(fieldx.stringValue());
                }

                fieldx=indexDoc.getField("uid");
                if(fieldx != null) {
                    indexDocument.setUID(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("summary");
                if(fieldx != null) {
                    indexDocument.setSummary(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("title");
                if(fieldx != null) {
                    indexDocument.setTitle(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("image");
                if(fieldx != null) {
                    indexDocument.setImage(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("price");
                if(fieldx != null) {
                    indexDocument.setPrice(fieldx.stringValue());
                }

                fieldx=indexDoc.getField("product");
                if(fieldx != null) {
                    indexDocument.setProduct(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("contents");
                if(fieldx != null) {
                    indexDocument.setContents(fieldx.stringValue());
                }

                fieldx=indexDoc.getField("modified");
                if(fieldx != null) {
                    indexDocument.setModifiedDate(fieldx.stringValue());
                }
                
                fieldx=indexDoc.getField("tag");
                if(fieldx != null) {
                    indexDocument.setTag(fieldx.stringValue());
                }

                fieldx=indexDoc.getField("disabled");
                if(fieldx != null) {
                    indexDocument.setDisabled(fieldx.stringValue());
                }
                
                // list all attributes indexed
                String outx="\nDocument" + indexDoc.toString() + "\n";

                if(bDebug) {
                    enumx=indexDoc.fields();
                    while(enumx.hasMoreElements()) {
                        fieldx=(Field)enumx.nextElement();
                        outx += "\tField - " + fieldx.toString() + "\n";
                        outx += "\t\t" + fieldx.name() + " - " +  fieldx.stringValue() + "\n";
                    }
                    System.out.println(outx);
                }
                
                hitsList.add(indexDocument);
            }
        } catch(Exception e) {
            PetstoreUtil.getLogger().log(Level.WARNING, "search.exception", e);
            e.printStackTrace();
        } finally {
            if(searcher != null) {
                try {
                    // make sure close search or index files get locked
                    searcher.close();
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        
        return hitsList;
    }
    
    public List<IndexDocument> getHits() {
            return hitsList;
    }
    
    public Hits getHitsNative() {
            return hits;
    }
    
    public static void main(String[] args) {
        // used for unit tests
        SearchIndex si=new SearchIndex();
        // use dummy default index file for unit tests
        si.query("/tmp/tmp/index", "Puppy");
    }
    
}
