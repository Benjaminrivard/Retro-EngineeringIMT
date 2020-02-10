/* Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: CatalogFacade.java,v 1.57 2007/01/17 18:00:07 basler Exp $ */

package com.sun.javaee.blueprints.petstore.model;

import com.sun.javaee.blueprints.petstore.search.IndexDocument;
import com.sun.javaee.blueprints.petstore.search.Indexer;
import com.sun.javaee.blueprints.petstore.search.UpdateIndex;
import com.sun.javaee.blueprints.petstore.util.PetstoreConstants;
import com.sun.javaee.blueprints.petstore.util.PetstoreUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
//import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.transaction.UserTransaction;
import java.util.logging.Level;


public class CatalogFacade implements ServletContextListener {
    // This class uses @SuppressWarnings annotation to supress the following kind of warnings: 
    // 
    // petstore/src/java/com/sun/javaee/blueprints/petstore/model/CatalogFacade.java:240: warning: [unchecked] unchecked conversion
    // found   : java.util.List
    // required: java.util.List<com.sun.javaee.blueprints.petstore.model.Product>
    //    .setParameter("categoryID", catID).getResultList();
    //
    // This is needed because the Query.getResultList() does not returns a generics version of objects. 
    // But since we are expecting a generic version (for example, List<Categories>), we need to
    // typecast the result appropriately. However, since generics information is lost at the runtime, 
    // there is no way to avoid a warning. Hence we use SuppressWarnings in this case
    
    @PersistenceUnit(unitName="PetstorePu") 
    private EntityManagerFactory emf;
    
    //@Resource 
    private UserTransaction utx;
    
    private static final boolean bDebug=false;
    
    public CatalogFacade(){ }
    
    public void contextDestroyed(ServletContextEvent sce) {
        //close the factory and all entity managers associated with it
        if (emf.isOpen()) emf.close();
    }
    
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        context.setAttribute("CatalogFacade", this);
    }
    
   @SuppressWarnings("unchecked") 
   public List<Category> getCategories(){
        EntityManager em = emf.createEntityManager();
        List<Category> categories = em.createQuery("SELECT c FROM Category c").getResultList();
        em.close();
        return categories;
    }
    
    @SuppressWarnings("unchecked") 
    public List<Product> getProducts(){
        EntityManager em = emf.createEntityManager();
        List<Product> products = em.createQuery("SELECT p FROM Product p").getResultList();
        em.close();
        return products;
    }
    
    @SuppressWarnings("unchecked") 
    public List<Item> getAllItemsFromCategory(String catID){
        EntityManager em = emf.createEntityManager();
        List<Item> items = em.createQuery("SELECT i FROM Item i, Product p WHERE i.productID = p.productID AND p.categoryID LIKE :categoryID AND i.disabled = 0")
        .setParameter("categoryID", catID).getResultList();
        em.close();
        return items;
    }
    
    /**
     * Value List Handler for items. The Chunk return contains an item with iID or nothing is returned. Uses the Java Persistence query language.
     * @param pID is the product id that the item belongs to
     * @param start position of the first result, numbered from 0
     * @param chunkSize the maximum number of results to retrieve
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemInChunkVLH(String pID, String iID, int chunkSize){
        EntityManager em = emf.createEntityManager();
        //make Java Persistence query
        Query query = em.createQuery("SELECT i FROM Item i WHERE i.productID = :pID AND i.disabled = 0");
        List<Item>  items;
        // scroll through these till we find the set with the itemID we are loooking for
        int index = 0;
        while (true) {
            items = query.setParameter("pID",pID).setFirstResult(index++ * chunkSize).setMaxResults(chunkSize).getResultList();
            if ((items == null) || items.size() <= 0) {
                break;
            }
            for (Item i : items) {
                // return this chunk if it contains the id we are looking for
                if (i.getItemID().equals(iID)) {
                    em.close();
                    return items;
                }
            }
        }
        em.close();
        return null;
    }
    
    /**
     * Value List Handler for items. Uses the Java Persistence query language.
     * @param pID is the product id that the item belongs to
     * @param start position of the first result, numbered from 0
     * @param chunkSize the maximum number of results to retrieve
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemsVLH(String pID, int start, int chunkSize){
        EntityManager em = emf.createEntityManager();
        
        //make Java Persistence query
        //Query query = em.createNamedQuery("Item.getItemsPerProductCategory");
        Query query = em.createQuery("SELECT i FROM Item i WHERE i.productID = :pID AND i.disabled = 0");
        List<Item>  items = query.setParameter("pID",pID).setFirstResult(start).setMaxResults(chunkSize).getResultList();
        em.close();
        return items;
    }
    
    /**
     * Value List Handler for items. Found by item ID
     * @param IDs is an array of item ids for specific items that need to be returned
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemsByItemID(String[] itemIDs){
        EntityManager em = emf.createEntityManager();
        List<Item>  items = new ArrayList<Item>();
        StringBuffer sbItemIDs=new StringBuffer();
        if(itemIDs.length !=0) {
            for(int i=0; i < itemIDs.length; ++i){
                sbItemIDs.append("'");
                sbItemIDs.append(itemIDs[i]);
                sbItemIDs.append("',");
            }
            // remove last comma
            String idString=sbItemIDs.toString();
            idString=idString.substring(0, idString.length() - 1);
            String queryString = "SELECT i FROM Item i WHERE " +
                    "i.itemID IN (" + idString + ")  AND i.disabled = 0";
            Query query = em.createQuery(queryString + " ORDER BY i.name");
            items = query.getResultList();
        }
        em.close();
        return items;
    }
    
    /**
     * Value List Handler for items. Found by item ID and radius
     * @param IDs is an array of item ids for specific items that need to be returned
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemsByItemIDByRadius(String[] itemIDs, double fromLat,
            double toLat, double fromLong, double toLong){
        EntityManager em = emf.createEntityManager();
        List<Item> items = new ArrayList<Item>();
        StringBuffer sbItemIDs=new StringBuffer();
        if(itemIDs.length !=0) {
            for(int i=0;i<itemIDs.length;++i){
                sbItemIDs.append("'");
                sbItemIDs.append(itemIDs[i]);
                sbItemIDs.append("',");
            }
            // remove last comma
            String idString=sbItemIDs.toString();
            idString=idString.substring(0, idString.length() - 1);
            
            String queryString = "SELECT i FROM Item i WHERE ((" +
                    "i.itemID IN (" +idString+"))";
            Query query = em.createQuery(queryString + " AND " +
                    " ((i.address.latitude BETWEEN :fromLatitude AND :toLatitude) AND " +
                    "(i.address.longitude BETWEEN :fromLongitude AND :toLongitude ))) AND i.disabled = 0" +
                    "  ORDER BY i.name");
            query.setParameter("fromLatitude",fromLat);
            query.setParameter("toLatitude",toLat);
            query.setParameter("fromLongitude",fromLong);
            query.setParameter("toLongitude",toLong);
            items = query.getResultList();
        }
        em.close();
        return items;
    }
    
    /**
     * Value List Handler for items. Found by category
     * @param categoryID is the category id that the item belongs to
     * @param start position of the first result, numbered from 0
     * @param chunkSize the maximum number of results to retrieve
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemsByCategoryVLH(String catID, int start,
            int chunkSize){
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT i FROM Item i, Product p WHERE " +
                "i.productID=p.productID AND p.categoryID = :categoryID AND i.disabled = 0" +
                " ORDER BY i.name");
        List<Item>  items = query.setParameter("categoryID",catID).setFirstResult(start).setMaxResults(chunkSize).getResultList();
        em.close();
        return items;
    }
    /**
     * Value List Handler for items. Found by category and location radius
     * @param categoryID is the category id that the item belongs to
     * @param start position of the first result, numbered from 0
     * @param chunkSize the maximum number of results to retrieve
     * @returns a List of Item objects
     */
    @SuppressWarnings("unchecked") 
    public List<Item> getItemsByCategoryByRadiusVLH(String catID, int start,
            int chunkSize,double fromLat,double toLat,double fromLong,
            double toLong){
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT i FROM Item i, Product p WHERE " +
                "i.productID=p.productID AND p.categoryID = :categoryID " +
                "AND((i.address.latitude BETWEEN :fromLatitude AND :toLatitude) AND " +
                "(i.address.longitude BETWEEN :fromLongitude AND :toLongitude )) AND i.disabled = 0" +
                "  ORDER BY i.name");
        query.setParameter("categoryID",catID);
        query.setParameter("fromLatitude",fromLat);
        query.setParameter("toLatitude",toLat);
        query.setParameter("fromLongitude",fromLong);
        query.setParameter("toLongitude",toLong);
        List<Item>  items = query.setFirstResult(start).setMaxResults(chunkSize).getResultList();
        em.close();
        return items;
    }
    
    /**
     * Gets a list of all the zipcode/city/state for autocomplete on user forms
     * Need to enhance so that returned list is cached for reuse at application scope
     * and held as member field of facade.
     * @returns a List of ZipLocation objects
     */
    @SuppressWarnings("unchecked") 
    public List<ZipLocation> getZipCodeLocations(String city, int start, int chunkSize){
        EntityManager em = emf.createEntityManager();
        String pattern = "'"+city.toUpperCase()+"%'";
        Query query = em.createQuery("SELECT  z FROM ZipLocation z where UPPER(z.city) LIKE "+pattern);
        List<ZipLocation>  zipCodeLocations = query.setFirstResult(start).setMaxResults(chunkSize).getResultList();
        em.close();
        return zipCodeLocations;
    }
    
    @SuppressWarnings("unchecked") 
    public List<Product> getProducts(String catID){
        EntityManager em = emf.createEntityManager();
        List<Product> products = em.createQuery("SELECT p FROM Product p WHERE p.categoryID LIKE :categoryID")
        .setParameter("categoryID", catID).getResultList();
        em.close();
        return products;
    }
    
    @SuppressWarnings("unchecked") 
    public List<Item> getItems(String prodID){
        EntityManager em = emf.createEntityManager();
        List<Item> items = em.createQuery("SELECT i FROM Item i WHERE i.productID LIKE :productID AND i.disabled = 0")
        .setParameter("productID", prodID).getResultList();
        em.close();
        return items;
    }
    
    public Category getCategory(String categoryID){
        EntityManager em = emf.createEntityManager();
        Category result = em.find(Category.class,categoryID);
        em.close();
        return result;
    }
    
    public Item getItem(String itemID){
        EntityManager em = emf.createEntityManager();
        Item result = em.find(Item.class,itemID);
        em.close();
        return result;
    }
    
    /**
     * Method to add an item with tags that are added using the addTag method
     *
     */
    public String addItem(Item item){
        EntityManager em = emf.createEntityManager();
        try{
            utx.begin();
            em.joinTransaction();
            for(Tag tag : item.getTags()) {
                tag.incrementRefCount();
                tag.getItems().add(item);
                em.merge(tag);
            }
            em.persist(item);
            utx.commit();
            // index item
            if(bDebug) System.out.println("\n***Item id of new item is : " + item.getItemID());
            indexItem(new IndexDocument(item));
            
        } catch(Exception exe){
            try {
                utx.rollback();
            } catch (Exception e) {}
            throw new RuntimeException("Error persisting item", exe);
        } finally {
            em.close();
        }
        return item.getItemID();
    }
    
    public void updateItem(Item item){
        EntityManager em = emf.createEntityManager();
        try{
            utx.begin();
            em.merge(item);
            utx.commit();

            // update index using delete/insert method (only one available)
            UpdateIndex.deleteIndex(PetstoreConstants.PETSTORE_INDEX_DIRECTORY, item.getItemID());
            indexItem(new IndexDocument(item));
        } catch(Exception exe){
            try {
                utx.rollback();
            } catch (Exception e) {}
            throw new RuntimeException("Error updating rating", exe);
        } finally {
            em.close();
        }
        

        
    }
    
    public Collection doSearch(String querryString){
        EntityManager em = emf.createEntityManager();
        Query searchQuery = em.createNativeQuery("SELECT * FROM Item WHERE (name LIKE ? OR description LIKE ?) AND disabled = 0" );
        searchQuery.setParameter(1, "%"+querryString+"%");
        searchQuery.setParameter(2,"%"+querryString+"%");
        
        Collection results = searchQuery.getResultList();
        em.close();
        return results;
    }
    
    
    public void addTagsToItemId(String sxTags, String itemId) {
        EntityManager em = emf.createEntityManager();
        // now parse tags for item
        Item item=getItem(itemId);
        StringTokenizer stTags=new StringTokenizer(sxTags, " ");
        String tagx=null;
        Tag tag=null;
        while(stTags.hasMoreTokens()) {
            tagx=stTags.nextToken().toLowerCase();
            if(!item.containsTag(tagx)) {
                // tag doesn't exist so add tag
                if(bDebug) System.out.println("Adding TAG = " + tagx);
                tag=addTag(tagx);
                //tag.incrementRefCount();
                tag.getItems().add(item);
                tag.incrementRefCount();
                item.getTags().add(tag);
            }
        }
        try {
            // persist data
            utx.begin();
            em.joinTransaction();
            em.merge(item);
            for( Tag tagz : item.getTags()) {
                if(bDebug) System.out.println("\n***Merging tag = " + tagz.getTag());
                em.merge(tagz);
            }
            utx.commit();
            
            // update indexes
            UpdateIndex update=new UpdateIndex();
            update.updateDocTag(PetstoreConstants.PETSTORE_INDEX_DIRECTORY, "tag" , item.tagsAsString(), item.getItemID(), UpdateIndex.REPLACE_FIELD);
            
        } catch(Exception exe){
            try {
                utx.rollback();
            } catch (Exception e) {}
            throw new RuntimeException("Error persisting tag", exe);
        } finally {
            em.close();
        }
    }
    
    
    @SuppressWarnings("unchecked") 
    public Tag addTag(String sxTag){
        EntityManager em = emf.createEntityManager();
        Tag tag=null;
        try {
            List<Tag> tags=em.createQuery("SELECT t FROM Tag t WHERE t.tag = :tag").setParameter("tag", sxTag).getResultList();
            if(tags.isEmpty()) {
                // need to create tag and set flag to add reference item
                tag=new Tag(sxTag);
                // persist data
                utx.begin();
                em.joinTransaction();
                em.persist(tag);
                utx.commit();
            } else {
                // see if item already exists in tag
                tag=tags.get(0);
            }
            
        } catch(Exception exe){
            try {
                utx.rollback();
            } catch (Exception e) {}
            throw new RuntimeException("Error persisting tag", exe);
        } finally {
            em.close();
        }
        return tag;
    }
    
    
    @SuppressWarnings("unchecked") 
    public List<Tag> getTagsInChunk(int start, int chunkSize) {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT t FROM Tag t ORDER BY t.refCount DESC, t.tag");
        List<Tag> tags = query.setFirstResult(start).setMaxResults(chunkSize).getResultList();
        em.close();
        return tags;
    }

    
    @SuppressWarnings("unchecked") 
    public Tag getTag(String sxTag) {
        Tag tag=null;
        EntityManager em = emf.createEntityManager();
        List<Tag> tags=em.createQuery("SELECT t FROM Tag t WHERE t.tag = :tag").setParameter("tag", sxTag).getResultList();
        em.close();
        if(tags != null && !tags.isEmpty()) {
            tag=tags.get(0);
        }
        return tag;
    }


    private void indexItem(IndexDocument indexDoc) {
        // Add document to index
        if(bDebug) System.out.println("\n*** document to index - " + indexDoc);
        Indexer indexer=null;
        try {
            indexer=new Indexer(PetstoreConstants.PETSTORE_INDEX_DIRECTORY, false);
            PetstoreUtil.getLogger().log(Level.FINE, "Adding document to index: " + indexDoc.toString());
            indexer.addDocument(indexDoc);
        } catch (Exception e) {
            PetstoreUtil.getLogger().log(Level.WARNING, "index.exception", e);
            e.printStackTrace();
        } finally {
            try {
                // must close file or will not be able to reindex
                if(indexer != null) {
                    indexer.close();
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }
    
}

