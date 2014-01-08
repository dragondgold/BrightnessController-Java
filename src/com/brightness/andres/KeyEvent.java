package com.brightness.andres;


public class KeyEvent{
	
	private boolean firstKeyPressed = false;
	private boolean secondKeyPressed = false;
	private boolean releaseAll;
	private boolean commandAssert;
	
	private String firstKey;
	private String secondKey;
	private String keyToRelease;
	private String identifier;
	
	private IOnKeyEvent mOnKeyEvent = null;
	
	public KeyEvent(String firstKey, String secondKey, String lastReleasedKey, boolean releaseAll, String identifier){
		this.firstKey = firstKey;
		this.secondKey = secondKey;
		this.keyToRelease = lastReleasedKey;
		this.releaseAll = releaseAll;
		this.identifier = identifier;
	}
	
	public void keyPressed(String key){
		if(key.equals(firstKey)) firstKeyPressed = true;
		else if(key.equals(secondKey)) secondKeyPressed = true;
		
		if(firstKeyPressed && secondKeyPressed) commandAssert = true;
		else commandAssert = false;
	}
	
	public void keyReleased(String key){
		if(key.equals(firstKey)) firstKeyPressed = false;
		else if(key.equals(secondKey)) secondKeyPressed = false;
		
		if(releaseAll && commandAssert){
			if(!firstKeyPressed && !secondKeyPressed){
				commandAssert = false;
				if(mOnKeyEvent != null) mOnKeyEvent.KeyEvent(identifier);
			}
		}
		else if(commandAssert){
			if(key.equals(keyToRelease)){
				if(mOnKeyEvent != null) mOnKeyEvent.KeyEvent(identifier);
			}
		}
	}
	
	public void addOnKeyEvent(IOnKeyEvent mOnKeyEvent){
		this.mOnKeyEvent = mOnKeyEvent;
	}
}
