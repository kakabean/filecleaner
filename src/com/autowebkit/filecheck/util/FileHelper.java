package com.autowebkit.filecheck.util;

import com.autowebkit.filecheck.filters.DirectoryFileFilter;
import com.autowebkit.filecheck.filters.ImageFileFilter;
import com.autowebkit.filecheck.filters.RecordFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hui on 2/13/16.
 */
public class FileHelper {

	ImageFileFilter imageFilter = new ImageFileFilter();
	
	DirectoryFileFilter dirFilter = new DirectoryFileFilter();
	
	/**
	 * file name with suffix 
	 * 
	 * @param filename
	 * @return like .abc
	 */
	public String getSuffix(String filename) {
		int i = filename.indexOf(".");
		if(i!=-1){
			return filename.substring(i,filename.length());
		}
		return "";
	}
	/**
	 * 
	 * @param filename
	 * @return 
	 */
	public String getFileName(String filename) {
		int i = filename.indexOf(".");
		if(i!=-1){
			return filename.substring(0,i);
		}
		return filename;
	}

	public List<File> getDirs(File f) {		
		return getFilesByFilter(f, dirFilter);
	}
	/**
	 * list all files under the folder matched the filter 
	 * 
	 * @param folder
	 * @param filter
	 * @return
	 */
	public List<File> getFilesByFilter(File folder, RecordFilter filter) {
		List<File> files = new ArrayList<File>();
		if (folder != null && folder.isDirectory()) {
			FileFilter ff = (FileFilter) filter ;
			File[] fs = folder.listFiles(ff);
			for (File file : fs) {
				files.add(file);
			}
		}
		
		return files; 
	}
}
