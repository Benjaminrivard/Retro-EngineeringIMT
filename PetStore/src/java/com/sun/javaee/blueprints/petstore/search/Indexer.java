/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: Indexer.java,v 1.9 2007/01/17 18:00:07 basler Exp $ */

package com.sun.javaee.blueprints.petstore.search;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * This class is used to encapsulate the Lucene functionality
 * for the Petstore application.  It is strictly used to loosely couple
 * the search with the Lucene engine for indexing
 *
 * @author basler
 */
public class Indexer {
    
    private IndexWriter writer=null; 
    
    /** Creates a new instance of Indexer */
    public Indexer(String file) throws IOException {
        this(file, true);
    }

    /** Creates a new instance of Indexer */
    public Indexer(String file, boolean create) throws IOException {
        writer=new IndexWriter(file, new StandardAnalyzer(), create);
        writer.setMaxFieldLength(1000000);
    }
    
    public void addDocument(IndexDocument indexDoc) throws IOException {
        // create an index document for the page
        Document doc=new Document();
        
        doc.add(new Field("url", indexDoc.getPageURL(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        doc.add(new Field("image", indexDoc.getImage(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        doc.add(new Field("price", indexDoc.getPrice(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        doc.add(new Field("product", indexDoc.getProduct(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        doc.add(new Field("uid", indexDoc.getUID(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        
        // Add the last modified date of the file a field named "modified".  Use a
        // Keyword field, so that it's searchable, but so that no attempt is made
        // to tokenize the field into words.
        doc.add(new Field("modified", indexDoc.getModifiedDate(), Field.Store.YES, Field.Index.UN_TOKENIZED));
        // use string return instead of reader, because info isn't retrievable which is 
        // needed for delete/add of document to index when tagging occurs
        //doc.add(Field.Text("contents", new StringReader(indexDoc.getContents())));
        doc.add(new Field("contents", indexDoc.getContents(), Field.Store.YES, Field.Index.TOKENIZED));        
        doc.add(new Field("title", indexDoc.getTitle(), Field.Store.YES, Field.Index.TOKENIZED));        
        doc.add(new Field("summary", indexDoc.getSummary(), Field.Store.YES, Field.Index.TOKENIZED));        
        doc.add(new Field("tag", indexDoc.getTag(), Field.Store.YES, Field.Index.TOKENIZED));        
        doc.add(new Field("disabled", indexDoc.getDisabled(), Field.Store.YES, Field.Index.UN_TOKENIZED));        
        
        writer.addDocument(doc);
    }
    
    
    public IndexWriter getWriter() {
        return writer;
    }
    
    public void close() throws IOException {
        writer.optimize();
        writer.close();
    }
    
}
