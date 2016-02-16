package com.autowebkit.filecheck.record;

import com.autowebkit.filecheck.listener.DuplicatedListChangedListener;
import com.autowebkit.filecheck.listener.IChangedListener;
import com.autowebkit.filecheck.util.RecordUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hui on 2/13/16.
 */
public class RecordIndex implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -595155517676700810L;

	HashMap<Long, FileRecord> directMap = new HashMap<Long, FileRecord>();

	private long totalItemSize = 0 ;
	
	/**
	 * get this record index size, how many FileRecord recorded, a FileRecord may
	 * marked more then two files  
	 *  
	 * @return
	 */
	public long getSize(){
		return directMap.size();
	}
	/**
	 * How many files are marked 
	 * 
	 * @return
	 */
	public long getTotalItemSize(){
		return totalItemSize ;
	}
	
	public Iterator<Long> getIterator(){
		return directMap.keySet().iterator();
	}
	
	public FileRecord get(Long key){
		return directMap.get(key);
	}
	
	public FileRecord put(Long key, FileRecord record){
		FileRecord r= directMap.put(key, record);
		
		addListener(record);
		
		long size = record.getTotalItemSize();
		totalItemSize += size ;
		
		return r ;
	}
	
	private void addListener(FileRecord record) {
		DuplicatedListChangedListener listener = new DuplicatedListChangedListener(this);
		record.addListener(listener);
	}
	
	public void remove(Long key){
		FileRecord r = directMap.remove(key);
		
		removeDuplicatedListChangedListener(r);
		
		long size = r.getTotalItemSize();
		totalItemSize -= size ;
		
		if(totalItemSize < 0){
			throw new RuntimeException("RecordIndex - remove(), key="+key+", internal error");
		}
	}
	
	private void removeDuplicatedListChangedListener(FileRecord r) {
		if(r!=null){
			IChangedListener[] listeners = r.getLisnters();
			List<IChangedListener> list = new ArrayList<IChangedListener>();
			for(IChangedListener l : listeners){
				if(l instanceof DuplicatedListChangedListener){
					list.add(l);
				}
			}
			
			for(IChangedListener l : list){
				r.removeListener(l);
			}
		}
	}
	public void clear() {
		directMap.clear();
		totalItemSize = 0 ;
	}
	
	public void putAll(HashMap<Long, FileRecord> map) {
		if(map!=null){
			Iterator<Long> kit = map.keySet().iterator();
			while(kit.hasNext()){
				Long key = kit.next() ;
				FileRecord r = map.get(key);
				this.put(key, r);				
			}
		}
	}
	
	public void recalcTotalItemSize() {
		totalItemSize = RecordUtil.getFileCount(directMap);
	}
}
