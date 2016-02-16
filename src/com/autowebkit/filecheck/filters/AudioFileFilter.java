package com.autowebkit.filecheck.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hui on 2/13/16.
 */
public class AudioFileFilter extends RecordFilter{
	
	private String TAG = "audio";
	
	public List<String> suffixs = new ArrayList<String>();
	
	public AudioFileFilter() {
		super();
		buildList();
	}

	private void buildList() {
		String[] ss = new String[]{".m4a",".amr",".wav"};
		for(String s : ss){
			suffixs.add(s);
		}
	}

	public boolean accept(File file) {
		if(file!=null && file.isFile()){
			String name = file.getName();
			String fn = name.toLowerCase();
			int index = fn.indexOf(".");
			if (index > 0 && fn.length()>3) {
				String sfx = fn.substring(index, fn.length());
				if (suffixs.contains(sfx)) {
					return true;
				}
			}
		}
		return false ;
	}

	public String getTag() {
		return TAG ;
	}

}
