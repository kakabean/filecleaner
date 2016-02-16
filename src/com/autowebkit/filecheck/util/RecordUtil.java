package com.autowebkit.filecheck.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.zip.Adler32;

import com.autowebkit.filecheck.filters.RecordFilter;
import com.autowebkit.filecheck.record.FileRecord;
import com.autowebkit.filecheck.record.RecordIndex;
import org.apache.commons.io.FileUtils;

/**
 * Created by hui on 2/13/16.
 */
public class RecordUtil {

	public static final String RECORD_FILE = "idx_";

	private static FileHelper fileHelper = new FileHelper();
	/**
	 * Get all FileRecord build for the folder and filters, and update the recordIndex file if necessary.
	 *  
	 * Note: 
	 * 1. first it will check whether there still a record index under the folder, if so, it will
	 *    do simple check(only the file last modified time and file size)
	 * 2. if no, it will rebuild the index map
	 * 
	 * @param rootFolder folder that all the filtered file will be recored
	 * @param filter filter
	 * @return RecordIndex object or null if errors. 
	 */
	public static HashMap<Long, FileRecord> getRecordMap(File rootFolder, RecordFilter filter){
		return getRecordMap(rootFolder, filter, false);
	}
	/**
	 * Get all FileRecord build for the folder and filters, and update the recordIndex file if necessary.
	 *  
	 * Note: 
	 * 1. first it will check whether there still a record index under the folder, if so, it will
	 *    do simple check(only the file last modified time and file size)
	 * 2. if no, it will rebuild the index map
	 * 
	 * @param rootFolder folder that all the filtered file will be recored
	 * @param filter filter
	 * @param forceBuild where force to rebuild the FileRecord map 
	 * @return RecordIndex object or null if errors. 
	 */
	public static HashMap<Long, FileRecord> getRecordMap(File rootFolder, RecordFilter filter, boolean forceBuild){
		if(forceBuild == false){
			return updateRecordIndex(rootFolder,filter);
		}else{
			// force update record Index 
			return buildRecordIndexFile(rootFolder, filter);
		}
	}
	/**
	 * whether the record index file updated 
	 * 
	 * @param rootFolder
	 * @param filter
	 * 
	 * @return true record index has been updated; false not updated  
	 */
	private static HashMap<Long, FileRecord> updateRecordIndex(File rootFolder, RecordFilter filter){
		if(rootFolder == null || !rootFolder.isDirectory()){
			Logger.log("Target folder is null");
			return null;
		}
		
		Stack<File> folders = new Stack<File>();
		if(rootFolder.isDirectory()){
			folders.push(rootFolder);
		}
		
		HashMap<Long,FileRecord> map = new HashMap<Long,FileRecord>();
		
		while(!folders.isEmpty()){
			File folder = folders.pop();
			
			long start = System.currentTimeMillis() ;
			
			// check whether there is a index file there 
			RecordIndex ri = readRecordIndex(folder, filter.getTag());
			boolean changed = false ;
			List<File> ffiles = fileHelper.getFilesByFilter(folder, filter);
			// no index file found
			if(ri == null || ri.getSize()==0){
				if (ffiles.size() > 0) {
					// build index file with direct files
					ri = buildSingleLevelRecordIndex(folder, filter);
					changed = true;
				}else{
					removeRecordIndexFile(folder, filter.getTag());
				}
			}else{
				// update index file with direct files if necessary
				changed = updateSingleLevelRecordIndex(ri,folder,filter);	
			}
			
			if(changed){
				writeRecordIndex(folder, ri, filter.getTag());
			}
			long rsize = 0 ;
			if(ri!=null){
				rsize = ri.getSize();
				updateRecordMap(map, ri);
			}
			long fileCounts = ri !=null ? ri.getTotalItemSize(): 0 ;
			long end = System.currentTimeMillis() ;
			Logger.log("Handle folder = "+folder.getPath()+", \trecord size = "+ rsize+", \tfiles = "
					+fileCounts+", \ttotal record size = "+map.size()+", \ttotal files = "+getFileCount(map)+", \ttime cost = "+(end-start)/1000+"s");
			
			List<File> dirs = fileHelper.getDirs(folder);
			if(! dirs.isEmpty()){
				for(File f : dirs){
					folders.push(f);
				}
			}			
		}

		return map;
	}
	
	/**
	 * get all file total count in the map 
	 * @param map
	 * @return all file count in the map 
	 */
	public static long getFileCount(HashMap<Long,FileRecord> map){
		long size = 0 ;
		if(map!=null){
			Iterator<Long> kit = map.keySet().iterator();
			while(kit.hasNext()){
				Long key = kit.next() ;
				FileRecord record = map.get(key);
				
				size+=record.getTotalItemSize();
			}
		}
		return size ;
	}
	/**
	 * Build FileRecord list file for target folder recursively, each folder will be created a 
	 * RecordIndex file 
	 * 
	 * @param rootFolder
	 * @param filter 
	 */
	private static HashMap<Long,FileRecord> buildRecordIndexFile(File rootFolder, RecordFilter filter){
		if(rootFolder == null || !rootFolder.isDirectory()){
			Logger.log("Target folder is null");
			return null ;
		}
		
		Stack<File> folders = new Stack<File>();
		if(rootFolder.isDirectory()){
			folders.push(rootFolder);
		}
		
		HashMap<Long,FileRecord> map = new HashMap<Long,FileRecord>();
		
		while(!folders.isEmpty()){
			File folder = folders.pop();
			
			long start = System.currentTimeMillis() ;
			
			removeRecordIndexFile(folder, filter.getTag());
			List<File> ffiles = fileHelper.getFilesByFilter(folder, filter);
			if (ffiles != null && ffiles.size() > 0) {
				RecordIndex ri = buildSingleLevelRecordIndex(folder, filter);
				// save record index file under folder
				writeRecordIndex(folder, ri, filter.getTag());

				updateRecordMap(map, ri);
				
				long end = System.currentTimeMillis() ;
				Logger.log("Handle folder = "+folder.getPath()+", \trecord = "+ ri.getSize()+", \ttotal record = "+map.size()+",\t Time cost = "+(end-start)/1000+"s");
			}
			List<File> dirs = fileHelper.getDirs(folder);
			if (!dirs.isEmpty()) {
				for(File f : dirs){
					folders.push(f);
				}
			}
		}
		
		return map ;
	}
	
	/**
	 * update record index direct map if necessary 
	 * 
	 * @param ri
	 * @param rootFolder
	 * @param filter
	 * @return true the record index has been updated
	 */
	private static boolean updateSingleLevelRecordIndex(RecordIndex ri, File rootFolder, RecordFilter filter) {
		boolean updated = false ;
		if (rootFolder.isDirectory()) {
			List<File> files = fileHelper.getFilesByFilter(rootFolder, filter);
			List<FileRecord> matchedList = new ArrayList<FileRecord>();
			List<FileRecord> new_added = new ArrayList<FileRecord>();
			for(File f : files){
				FileRecord r = RecordUtil.getRecord(ri, f);
				if (r!=null) {
					matchedList.add(r);
				}else{
					FileRecord record = RecordUtil.buildFileRecord(f);
					if (record != null) {
						new_added.add(record);
						updated = true;
					}
				}
			}
			
			long oldsize = 0 ;
			Iterator<Long> kit = ri.getIterator();
			while(kit.hasNext()){
				Long key = kit.next();
				FileRecord fr = ri.get(key);
				oldsize += fr.getTotalItemSize() ;				
			}
			
			if(matchedList.size()==oldsize && new_added.size() == 0){
				return false ;
			}else{
				HashMap<Long,FileRecord> matched = buildFileRecordMapFromList(matchedList);
				ri.clear();
				ri.putAll(matched);
				for(FileRecord fr : new_added){
					updateDirectRecord(ri, fr);
				}
				updated = true ;
			}
		}
		
		return updated; 
	}
	
	private static HashMap<Long, FileRecord> buildFileRecordMapFromList(List<FileRecord> recordList) {
		HashMap<Long,FileRecord> matched = new HashMap<Long,FileRecord>();
		if(recordList!=null && recordList.size()>0){
			for(FileRecord fr : recordList){
				FileRecord record = matched.get(fr.getCheckSum());
				if( record == null){
					matched.put(fr.getCheckSum(), fr);
				}else{
					record.addDuplicateRecord(fr);
				}
			}
		}
		return matched;
	}
	/**
	 * fast check whether the f has been record in the map, only compare the size and last modified time
	 * 
	 * @param rindex
	 * @param f
	 * @return
	 */
	private static FileRecord getRecord(RecordIndex rindex, File f) {
		if(rindex!=null && f!= null && f.isFile()){
			if(rindex.getTotalItemSize() > 200 && f.length() < 10000000){
				FileRecord ff = buildFileRecord(f);
				if(ff!=null){
					FileRecord r = rindex.get(ff.getCheckSum());
					return findMatched(f, r);
				}
			}else{
				Iterator<Long> kit = rindex.getIterator();
				while (kit.hasNext()) {
					Long key = kit.next();
					FileRecord r = rindex.get(key);
					FileRecord findRecord = findMatched(f, r);
					if (findRecord != null) {
						return findRecord;
					}
				}
			}
		}
		return null;
	}
	
	private static FileRecord findMatched(File f, FileRecord r) {
		if(r.getFilesize() == f.length() && r.getLastModified() == f.lastModified()
				&& r.getFilepath().equals(f.getPath())){
			return r ;
		}
		
		// further more, check the duplicated list 
		Iterator<FileRecord> rit = r.getDuplicatedIterator() ;
		while(rit.hasNext()){
			FileRecord fr = rit.next() ;
			if(fr.getFilesize() == f.length() && fr.getLastModified() == f.lastModified()
					&& fr.getFilepath().equals(f.getPath())){
				return fr ;
			}	
		}
		
		return null ;
	}

	private static void updateRecordMap(HashMap<Long,FileRecord> totalMap, RecordIndex rindex){
		Iterator<Long> kit = rindex.getIterator();
		while(kit.hasNext()){
			Long checksum = kit.next();
			FileRecord subRecord = rindex.get(checksum);
			FileRecord record = totalMap.get(checksum);
			if(record!=null){ // duplicated item
				updateNewRecord(record, subRecord);
			}else{
				totalMap.put(checksum, subRecord);
			}
		}
	}

	/**
	 * Build only files under the rootFolder with filters
	 * 
	 * @param rootFolder
	 * @param filter
	 * @return recordIndex object 
	 */
	private static RecordIndex buildSingleLevelRecordIndex(File rootFolder, RecordFilter filter) {
		RecordIndex recordIndex = new RecordIndex();

		long start = System.currentTimeMillis() ;
		long totalStart = start ; 
		
		if (rootFolder.isDirectory()) {
			List<File> files = fileHelper.getFilesByFilter(rootFolder, filter);
			for(int i=0; i< files.size(); i++){
				File f = files.get(i);			
				FileRecord record = RecordUtil.buildFileRecord(f);
				if(record!=null){
					updateDirectRecord(recordIndex,record);
				}
				
				if (Logger.getLevel() <= Logger.DEBUG) {
					long end = System.currentTimeMillis();
					long time = end - start;
					start = end;
					Logger.log("    -> build record, index size = "+recordIndex.getSize()+", \ttotalItemSize="+recordIndex.getTotalItemSize()
										+", \tfiles left = " + (files.size() - i) + ", \tsize = " + (f.length() / 1000000) 
										+ "M, \tcost = " + time + "ms, \ttotal cost = "+(end-totalStart)/1000+ "s, \tpath=" + f.getPath() , Logger.DEBUG);
				}
			}
		}
		
		return recordIndex ;
	}

	public static FileRecord buildFileRecord(File file) {
		FileRecord record = null;
		try {			
			Long md5 = FileUtils.checksum(file, new Adler32()).getValue();
			
			record = new FileRecord();
			record.setCheckSum(md5);
			record.setFileName(file.getName());
			record.setFilepath(file.getPath());
			record.setFilesize(file.length());
			record.setLastModified(file.lastModified());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}
	
	private static boolean removeRecordIndexFile(File tgtFolder, String tag){
		if(tgtFolder == null || !tgtFolder.isDirectory()){
			Logger.log("removeRecordIndexFile - Target folder is null");
		}
		File indexFile = getIndexFile(tgtFolder, tag);
		
		if(indexFile.exists()){
			Logger.log("   remove index file, file = "+indexFile.getPath());
			indexFile.delete();
		}
		
		return true ;
	}
	
	private static File getIndexFile(File tgtFolder, String tag) {
		return new File(tgtFolder.getPath()+File.separator+RECORD_FILE+ tag);
	}
	/**
	 * write recordIndex file for the target folder 
	 * 
	 * @param tgtFolder
	 * @param tag
	 * @return
	 */
	private static RecordIndex writeRecordIndex(File tgtFolder, RecordIndex recordIndex, String tag){
		if(tgtFolder == null || !tgtFolder.isDirectory()){
			Logger.log("Target folder is null");
		}
		File indexFile = getIndexFile(tgtFolder, tag);
		
		removeRecordIndexFile(tgtFolder, tag);
				
		Logger.log("   write  index file, file = "+indexFile.getPath());
				
		RecordIndex rindex = recordIndex;				
		FileOutputStream fos = null ;
		ObjectOutputStream oos = null ;
		try {
			fos = new FileOutputStream(indexFile);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(recordIndex);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(oos!=null){
				try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return rindex ;
	}
	
	/**
	 * read the recordIndex under the target folder  
	 * 
	 * @param targetFolder
	 * @param tag
	 * @return RecordIndex object under the target folder of null if errors 
	 */
	private static RecordIndex readRecordIndex(File targetFolder, String tag){
		if(targetFolder == null || !targetFolder.isDirectory()){
			Logger.log("Target folder is null");
		}
		File indexFile = getIndexFile(targetFolder, tag);
		
		if(!indexFile.exists()){
			Logger.log("   readRecord info, Index file not found , file = "+indexFile.getPath());
			return null ;
		}
		
		RecordIndex rindex = null;				
		FileInputStream fis = null ;
		ObjectInputStream ois = null ;
		try {
			fis = new FileInputStream(indexFile);
			ois = new ObjectInputStream(fis);
			rindex = (RecordIndex)ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally{
			if(ois!=null){
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return rindex ;
	}

	private static void updateDirectRecord(RecordIndex recordIndex, FileRecord record) {
		Long key = record.getCheckSum();
		FileRecord r = recordIndex.get(key);
		
		if(r == null){
			recordIndex.put(key, record);
		}else{
			updateNewRecord(r,record);
		}
	}
	
	/**
	 * update the newRecord info into existed record
	 * @param record
	 * @param newRecord
	 */
	private static void updateNewRecord(FileRecord record, FileRecord newRecord){
		if(record!=null && newRecord!=null && record.getCheckSum() == newRecord.getCheckSum()){
			boolean find = false ;
			if(record.isTotallySame(newRecord)){
				// find newRecord is totally same file with the record marked file 
				return ;
			}
			
			Iterator<FileRecord> rit = record.getDuplicatedIterator();
			while(rit.hasNext()){
				FileRecord r = rit.next() ;
				if(r.isTotallySame(newRecord)){
					find = true ;
				}
			}
			
			if(find == false){
				record.addDuplicateRecord(newRecord);
			}
		}
	}
	
}
