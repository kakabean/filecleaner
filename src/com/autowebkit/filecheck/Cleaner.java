package com.autowebkit.filecheck;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.autowebkit.filecheck.filters.AudioFileFilter;
import com.autowebkit.filecheck.filters.ImageFileFilter;
import com.autowebkit.filecheck.filters.RecordFilter;
import com.autowebkit.filecheck.filters.VedioFileFilter;
import com.autowebkit.filecheck.record.FileRecord;
import com.autowebkit.filecheck.util.CleanerUtil;
import com.autowebkit.filecheck.util.Logger;
import com.autowebkit.filecheck.util.RecordUtil;

/**
 * Created by hui on 2/13/16.
 */
public class Cleaner {
	
	private static final String srcpath = "E:\\photoes_raw";
	
	private static final String tgtpath = "E:\\photoes_ok";
	
	private static final String duplicatedpath = "E:\\duplicated";
	
	private List<RecordFilter> filters = new ArrayList<RecordFilter>();
	
	private void init(){
		ImageFileFilter imageFilter = new ImageFileFilter();
		AudioFileFilter audioFilter = new AudioFileFilter();
		VedioFileFilter vedioFilter = new VedioFileFilter();
		
		filters.add(imageFilter);
		filters.add(audioFilter);
		filters.add(vedioFilter);
	}
	
	public Cleaner() {
		super();
		init();
	}

	public static void main(String[] args) throws IOException {
		Cleaner cleaner = new Cleaner();
		
		File tgtFolder = new File(tgtpath);
		File dupFolder = new File(duplicatedpath);
		File srcFolder = new File(srcpath);
		
		long start = System.currentTimeMillis() ;
		
		StringBuilder sb = new StringBuilder();
		
		for (RecordFilter filter : cleaner.filters) {			
			long s1 = System.currentTimeMillis() ;
			
			HashMap<Long, FileRecord> tgtmap = cleaner.getRecordMap(tgtFolder, filter);
			HashMap<Long, FileRecord> srcmap = cleaner.getRecordMap(srcFolder, filter);
			
			MergeResult result = cleaner.mergeSrcIntoTgt(srcmap, tgtmap);
			
			String log = "after merge, filter = "+filter.getTag()+", result.deltamap size = "+result.getDeltamap().size()
					+", result.dupmap size = "+result.getDupmap().size()+", tgt files = "+ RecordUtil.getFileCount(tgtmap)
					+", src files = "+RecordUtil.getFileCount(srcmap);
			sb.append(log).append("\n");
			Logger.log(log);
			
			CleanerUtil.reorg(result.getDeltamap(), tgtFolder, dupFolder);
			CleanerUtil.handleDupRecord(result.getDupmap(), dupFolder);
			
			long e1 = System.currentTimeMillis();
			long cost = (e1-s1)/1000 ;
			log = "## finished, filter = "+filter.getTag()+" ##, time cost = "+cost+"s";
			sb.append(log).append("\n\n");
			Logger.log(log);
		}
		
		long time = (System.currentTimeMillis() - start)/1000 ;
		String logtxt = " =============== All finished ======== total time cost = "+time+"s" ;
		sb.append(logtxt).append("\n\n\n");
		Logger.log(sb.toString());
		
		// start test		
		for (RecordFilter filter : cleaner.filters) {			
			HashMap<Long, FileRecord> tgtmap = cleaner.getRecordMap(tgtFolder, filter);	
			HashMap<Long, FileRecord> dupmap = cleaner.getRecordMap(dupFolder, filter);
			HashMap<Long, FileRecord> newmap = new HashMap<Long, FileRecord>();
			Iterator<Long> kit = dupmap.keySet().iterator();
			while(kit.hasNext()){
				Long key = kit.next();
				FileRecord record = tgtmap.get(key);
				if(record == null){
					newmap.put(key, dupmap.get(key));
				}
			}
			sb.append("Verification finished, filter = "+filter.getTag()+" , new map size = "+newmap.size()
			+", tgt files = "+RecordUtil.getFileCount(tgtmap)+", dup files = "+RecordUtil.getFileCount(dupmap)).append("\n");
			
			if(newmap.size()>0){
				CleanerUtil.printMap(newmap);
			}
		}
		
		Logger.log(sb.toString());
		
		// end test
	}

	private HashMap<Long, FileRecord> getRecordMap(File folder, RecordFilter filter) {
		// build target folder map info
		long start = System.currentTimeMillis();
		HashMap<Long, FileRecord> map = RecordUtil.getRecordMap(folder, filter);
		long sec = (System.currentTimeMillis() - start) / 1000;
		Logger.log("---- Finished, " + ", filter = " + filter.getTag() + ", total map = " + map.size()
				+ ", file counts = " + RecordUtil.getFileCount(map) + ", time cost = " + sec + "(seconds)");
		
		return map ;
	}
	/**
	 * The delta map will record the files that will be move into target folder 
	 * and the duplicated folder 
	 * 
	 * @param srcmap
	 * @param tgtmap
	 * @return
	 */
	public MergeResult mergeSrcIntoTgt(HashMap<Long, FileRecord> srcmap,
			HashMap<Long, FileRecord> tgtmap) {
		MergeResult result = new MergeResult();
		Iterator<Long> kit = srcmap.keySet().iterator();
		while(kit.hasNext()){
			Long key = kit.next();
			FileRecord fr = tgtmap.get(key);
			if(fr == null){
				result.getDeltamap().put(key, srcmap.get(key));
			}else{
				result.getDupmap().put(key, srcmap.get(key));
			}
		}
		
		return result ;
	}

	class MergeResult{
		/**
		 * delta map contains the newly added files that in src folder but not find in target folder 
		 */
		HashMap<Long, FileRecord> deltamap = new HashMap<Long, FileRecord>();
		/**
		 * contains the record that in src folder and it is duplicated with a target folder one
		 */
		HashMap<Long, FileRecord> dupmap = new HashMap<Long, FileRecord>();
		
		public HashMap<Long, FileRecord> getDeltamap() {
			return deltamap;
		}
		public HashMap<Long, FileRecord> getDupmap() {
			return dupmap;
		}
		
	}
	
}
