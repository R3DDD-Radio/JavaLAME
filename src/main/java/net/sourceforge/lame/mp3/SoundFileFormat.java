package net.sourceforge.lame.mp3;

public enum SoundFileFormat {
	
	UNKNOWN,
	RAW,
	WAVE,
	AIFF,
	
	/**
	 * MPEG Layer 1, aka mpg
	 */
	MP1,
	
	/**
	 * MPEG Layer 2
	 */
	MP2,
	
	/**
	 * MPEG Layer 3
	 */
	MP3,
	
	/**
	 * MPEG Layer 1,2 or 3; whatever .mp3, .mp2, .mp1 or .mpg contains
	 */
	MP123
}
