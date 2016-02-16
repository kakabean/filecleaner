package com.autowebkit.filecheck.listener;

import com.autowebkit.filecheck.record.FileRecord;
import com.autowebkit.filecheck.record.RecordIndex;

/**
 * Created by hui on 2/13/16.
 */
public class DuplicatedListChangedListener implements IChangedListener {
	
	RecordIndex rindex = null ;

	public DuplicatedListChangedListener(RecordIndex rindex) {
		super();
		this.rindex = rindex;
	}

	@Override
	public void validate(Object obj) {
		if(obj instanceof FileRecord){
			FileRecord record = (FileRecord)obj;
			FileRecord r = rindex.get(record.getCheckSum());
			if(r!=null){
				rindex.recalcTotalItemSize();
			}
		}
	}
}
