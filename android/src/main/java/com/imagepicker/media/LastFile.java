package com.imagepicker.media;

import java.io.File;

/**
 * Author: Chandler Newman-Reed
 * Project: imagepicker
 * Date: 9/19/2017
 * Purpose: Created to help fix the issue with putExtra (on different devices) when taking
 *          images or videos with ACTION_CAPTURE.
 */

public class LastFile {
    private long size;
    private int id;
    private String path;

    public LastFile(){
        size = 0;
        id = -1;
    }

    public LastFile(int id, long size, String path){
        this.size = size;
        this.id = id;
        this.path = path;
    }

    public long getSize(){
        return size;
    }

    public int getId(){
        return id;
    }

    public String getPath(){
        return path;
    }

    public File getFile(){
        return new File(path);
    }

    /*
     * Not exactly used but is there just incase
     */
    public boolean equals(LastFile lastFile){
        if(this.size == lastFile.getSize() && this.id == lastFile.getId()){
            return true;
        }
        return false;
    }

    /*
     * If last image id in the media store is greater than the id of the new image than we
     * can assume that the image is a new image
     */
    public boolean notLastFile(LastFile lastFile){
        if(this.id > lastFile.getId()){
            return true;
        }
        return false;
    }
}
