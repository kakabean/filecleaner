/**
 * 
 */
package com.autowebkit.filecheck.util;

/**
 * Created by hui on 2/13/16.
 */
public class Logger {
	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int WARNING = 2;
	public static final int ERROR = 3; 
	
	public static int USER_LOG_LEVEL = 0 ;
	
	public static void log(String msg, int LEVEL){
		if(USER_LOG_LEVEL >= LEVEL){
			System.out.println(msg);
		}
	}
	
	public static void log(String msg){
		log(msg, USER_LOG_LEVEL);
	}
	
	public static int getLevel(){
		return USER_LOG_LEVEL ;
	}
}
