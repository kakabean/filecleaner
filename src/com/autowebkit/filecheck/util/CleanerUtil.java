package com.autowebkit.filecheck.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import com.autowebkit.filecheck.record.FileRecord;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;

/**
 * Created by hui on 2/13/16.
 */
public class CleanerUtil {

	private static FileHelper fileHelper = new FileHelper();
	
	/**
	 * re-organize the files align with the map, the unique file will be moved totargetFolder under a folder like
	 * File_Month+current parent folder, like 201502pic, 
	 * and all the duplicated files will the put under the duplicated folder with similar names 
	 * 
	 * @param recordMap FileRecord map
	 * @param targetFolder re-organize files destination folder without duplicated files 
	 * @param dupDir duplicated files folder 
	 * @throws IOException
	 */
	public static void reorg(HashMap<Long, FileRecord> recordMap, File targetFolder, File dupDir) throws IOException {
		if(!dupDir.isDirectory()){
			dupDir.mkdirs();
		}
		
		Iterator<Long> it = recordMap.keySet().iterator();
		while(it.hasNext()){
			Long checkSum = it.next();
			FileRecord record = recordMap.get(checkSum);
	
			try {
				moveNewFile(targetFolder, record);

				Iterator<FileRecord> rit = record.getDuplicatedIterator();
				while(rit.hasNext()){
					FileRecord dup_record = rit.next() ;
					moveDupFile(dupDir, dup_record);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}	
		}
	}
	/**
	 * Move all the files marked by dupMap into the dupDir folders. 
	 * 
	 * @param dupMap
	 * @param dupDir
	 * @throws IOException
	 */
	public static void handleDupRecord(HashMap<Long, FileRecord> dupMap, File dupDir) throws IOException {
		if(!dupDir.isDirectory()){
			dupDir.mkdirs();
		}
		
		Iterator<Long> it = dupMap.keySet().iterator();
		while(it.hasNext()){
			Long checkSum = it.next();
			FileRecord record = dupMap.get(checkSum);
	
			try {
				moveDupFile(dupDir, record);

				Iterator<FileRecord> rit = record.getDuplicatedIterator();
				while(rit.hasNext()){
					FileRecord dup_record = rit.next() ;
					moveDupFile(dupDir, dup_record);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}	
		}
	}
	/**
	 * move the record marked file into target foder, only move the record directly marked 
	 * file, NOT about the duplicated files 
	 * 
	 * @param targetFolder
	 * @param record
	 * @throws IOException
	 */
	private static void moveNewFile(File targetFolder, FileRecord record) throws IOException {
		Random rnd = new Random();
		
		File srcFile = new File(record.getFilepath());
		String folder = srcFile.getParentFile().getName();
		
		String fpath = targetFolder.getPath() + File.separator + record.getLastModifiedMonth()+folder;
		File destDir = new File(fpath);
		// move unique image files 
		try{
			Logger.log("mv from path="+srcFile.getPath()+", to = "+destDir);
			FileUtils.moveFileToDirectory(srcFile, destDir, true);
		}catch(FileExistsException fee){
			File destFile = new File(destDir.getPath()+File.separator+srcFile.getName());
			FileRecord fr = RecordUtil.buildFileRecord(destFile);
			if(!record.isSame(fr)){
				String pname = fileHelper.getFileName(srcFile.getName());
				String suffix = fileHelper.getSuffix(srcFile.getName());
				long rd = rnd.nextInt()%1000;
				File tf = new File(destDir.getPath()+File.separator+pname+rd+suffix);
				FileUtils.moveFile(srcFile, tf);
			}
		}
	}
	
	/**
	 * Move the duplicated record marked file into duplicated folder, only handle the record
	 * itself, NOT include the duplicated list. 
	 * 
	 * @param dupDir
	 * @param dupRecord
	 * @throws IOException
	 */
	private static void moveDupFile(File dupDir, FileRecord dupRecord) throws IOException {
		Random rnd = new Random();
		File sdf = new File(dupRecord.getFilepath());
		// new duplicated file path 
		File newf = new File(dupDir.getPath() + File.separator + sdf.getName());
		if (newf.exists()) {
			String pname = fileHelper.getFileName(newf.getName());
			String suffix = fileHelper.getSuffix(newf.getName());
			String parent = "";
			if(sdf.getParentFile().exists()){
				parent+=sdf.getParentFile().getName();
			}
			long i1 = System.currentTimeMillis()%1000;
			long i2 = rnd.nextInt()%1000;
			i2 = i2 < 0 ? 0-i2 : i2 ;
			newf = new File(dupDir.getPath() + File.separator + pname+"_pn_"+parent+"_dup"+i1+"_"+i2+suffix);
		} 
		Logger.log("mv from path="+sdf.getPath()+", to = "+newf);
		FileUtils.moveFile(sdf, newf);	
	}
	
	public static void printMap(HashMap<Long, FileRecord> hashmap) {
		System.out.println("total size = "+hashmap.size());
		Iterator<Long> it = hashmap.keySet().iterator();
		while(it.hasNext()){
			Long checkSum = it.next();
			FileRecord record = hashmap.get(checkSum);
			System.out.println(record.toString());
		}
	}

	
}
