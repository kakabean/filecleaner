/**
 * 
 */
package com.autowebkit.filecheck.record;

import com.autowebkit.filecheck.listener.IChangedListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * FileRecord is used to identify a file
 *
 * Created by hui on 2/13/16.
 */
public class FileRecord implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6935065040983122516L;

	String fileName = null ;
	
	String filepath = null ;
	
	long checkSum = 0L ;
	
	long lastModified = 0L;
	
	long filesize = 0L;

	List<FileRecord> duplicateList = new ArrayList<FileRecord>();
	
	private transient List<IChangedListener> listeners = null ;

	public FileRecord() {
		super();
		listeners = new ArrayList<IChangedListener>();
	}

	public void addListener(IChangedListener listener){
		if(listener!=null){
			
			for(IChangedListener l : listeners){
				if(l.getClass().equals(listener.getClass())){
					listeners.remove(l);
					break ;
				}
			}
			
			this.listeners.add(listener);
		}
	}
	
	public void removeListener(IChangedListener listener){
		if(listener!=null){
			this.listeners.remove(listener);
		}
	}
	
	public void notifyChanged(){
		for(IChangedListener ls : this.listeners){
			ls.validate(this);
		}
	}
	
	public IChangedListener[] getLisnters(){
		return (IChangedListener[])listeners.toArray();
	}
	
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilepath() {
		return filepath;
	}
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
	
	public long getCheckSum() {
		return checkSum;
	}
	public void setCheckSum(long checkSum) {
		this.checkSum = checkSum;
	}

	public long getFilesize() {
		return filesize;
	}
	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}
	/**
	 * whether the record marked file is same with the current record. 
	 * This is used to check whether the two file is the same file(maybe with same name or under different path)
	 * 
	 * @param record
	 * @return
	 */
	public boolean isSame(FileRecord record){
		if(record!=null){
			return checkSum == record.checkSum && lastModified == record.lastModified && filesize ==record.filesize;
		}
		return false; 
	}
	/**
	 * whether the record marked file is total the same file with the current record. 
	 * 
	 * @param record
	 * @return
	 */
	public boolean isTotallySame(FileRecord record ){
		if(this.isSame(record) && this.getFilepath().equals(record.getFilepath())){
			// find newRecord is totally same file with the record marked file 
			return true;
		}
		return false ;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("FileRecord [fileName=" + fileName + ", filesize=" + filesize + ", checkSum="
				+ checkSum +", lastModified = "+getLastModifiedDate()+", filepath=" + filepath +"]");
		if(this.duplicateList!=null){
			for(FileRecord r : duplicateList){
				sb.append("\n          =>fileName=" + r.fileName + ", filepath=" + r.filepath+", lastModified = "+getLastModifiedDate());
			}
		}
		
		return sb.toString();
	}

	public Iterator<FileRecord> getDuplicatedIterator(){
		return this.duplicateList.iterator() ;
	}
	/**
	 * How many files marked by the record, both record itself and duplicated files.
	 * 
	 * @return
	 */
	public long getTotalItemSize(){
		int size = 1 ;
		if(this.duplicateList!=null && this.duplicateList.size() > 0){
			size += this.duplicateList.size();
		}
		return size ;
	}
	public void addDuplicateRecord(FileRecord record) {
		if(record!=null){
			if(record.duplicateList!=null && record.duplicateList.size()>0){				
				for(FileRecord fr : record.duplicateList){
					this.doAddDuplicatedRecord(fr);
				}
				record.clearDuplicated();
			}
			
			boolean updated = this.doAddDuplicatedRecord(record);
			if(updated){
				this.notifyChanged();
			}
		}
	}
	
	private boolean doAddDuplicatedRecord(FileRecord record){
		if(this.duplicateList!=null){
			for(FileRecord f : this.duplicateList){
				if(f.isTotallySame(record)){
					return false;
				}
			}
		}
		
		this.duplicateList.add(record);
		return true ;
	}
	
	private void clearDuplicated() {
		this.duplicateList.clear();
	}
	
	public boolean removeDuplicateRecord(FileRecord record) {
		if(record!=null && record.getCheckSum() == this.getCheckSum()){
			List<FileRecord> removed = new ArrayList<FileRecord>();
			
			if(record.duplicateList!=null && record.duplicateList.size()>0){
				List<FileRecord> tlist = new ArrayList<FileRecord>();
				tlist.add(record);
				tlist.addAll(record.duplicateList);
				for(FileRecord todoR: tlist){
					for(FileRecord sr: this.duplicateList){
						if(sr.isTotallySame(todoR)){
							removed.add(sr);
						}
					}
				}

			}
			if(removed.size()>0){
				this.duplicateList.removeAll(removed);
				this.notifyChanged();
				return true ;
			}
		}
		return false ;
	}
	
	/**
	 * 
	 * @return file last modified date, like "201601"
	 */
	public String getLastModifiedDate(){
		Date date = new Date(this.lastModified);
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		
		return sdf.format(date);
	}
	/**
	 * 
	 * @return file last modified date, like "201601"
	 */
	public String getLastModifiedMonth(){
		Date date = new Date(this.lastModified);
		DateFormat sdf = new SimpleDateFormat("yyyyMM");
		
		return sdf.format(date);
	}
	
	private void readObject(ObjectInputStream in) throws IOException,ClassNotFoundException{
        in.defaultReadObject();
        this.listeners = new ArrayList<IChangedListener>();
    }
}
