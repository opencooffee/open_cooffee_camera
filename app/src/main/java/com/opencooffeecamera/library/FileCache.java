package com.opencooffeecamera.library;

import android.content.Context;

import java.io.File;

public class FileCache {
	
	//private static final String LOG_TAG = FileCache.class.getSimpleName();
    
    private static File cacheDir;
    
    // Constructor.
    public FileCache(Context context) {
    	
        cacheDir = context.getCacheDir();
    		    		
    	if (cacheDir != null && !cacheDir.exists()) {
            cacheDir.mkdirs();
        }

    }
    
    /**
     * Get File by name.
     *
     * @param fileName
     * The name of the file.
     * 
     * @return File
     */
    public File getFile(String fileName) {

    	try {  
    		     	             
        	return new File(cacheDir, fileName);

    	} catch (NullPointerException e) {

            // if the name is null.
    		e.printStackTrace();
    		
    		return null;
    	}
        
    }
    
    /**
     * Delete a file in the cache memory by name.
     * 
     * @param fileName
     * The name of the file.
     *
     * @return Boolean value
     */
    public boolean deleteFileByName(String fileName) {
    	
    	File file = new File(cacheDir, fileName);
    	    	
    	return file.exists() && !file.isDirectory() && file.delete();
    }
    
    /**
     * Delete all cache memory.
     */
    public void delete() {
    	
        File[] files = cacheDir.listFiles();
        
        if (files == null) {
            return;
        }
        
        for (File file: files) {

        	if (file.exists() && !file.isDirectory()) {
        		file.delete();
        	}

        }
        
    }

}