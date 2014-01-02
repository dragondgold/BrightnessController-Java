package com.brightness.andres;

import java.util.Properties;

public class Settings extends Properties{

	private static final long serialVersionUID = 1L;

	public synchronized void setProperty(String key, int value){
		super.setProperty(key, ""+value);
	}
	
	public synchronized int getInt(String key){
		return Integer.parseInt(super.getProperty(key));
	}
	
}
