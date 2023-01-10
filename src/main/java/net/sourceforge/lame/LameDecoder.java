package net.sourceforge.lame;

import net.sourceforge.lame.mp3.*;

import java.io.DataInputStream;

public class LameDecoder {
	
	private final Lame lame;
	
	private final float[][] decodeBuffer = new float[2][1152];
	
	public LameDecoder(String filename, DataInputStream dataInputStream) {
		lame = new Lame();
		lame.getFlags().setWriteId3tagAutomatic(false);
		lame.initParams();
		lame.getParser().initializeInputFormat(filename);
		lame.getAudio().initialize(lame.getFlags(), dataInputStream, new FrameSkip());
	}
	
	public boolean decode(byte[] sampleBuffer) {
		LameGlobalFlags flags = lame.getFlags();
		int iread = lame.getAudio().getAudio16(flags, decodeBuffer);
		if(iread >= 0) {
			for(int i = 0; i < iread; i++) {
				int sample = ((int) decodeBuffer[0][i] & 0xffff);
				sampleBuffer[(i << flags.getInNumChannels()) + 0] = (byte) (sample & 0xff);
				sampleBuffer[(i << flags.getInNumChannels()) + 1] = (byte) ((sample >> 8) & 0xff);
				if(flags.getInNumChannels() == 2) {
					sample = ((int) decodeBuffer[1][i] & 0xffff);
					sampleBuffer[(i << flags.getInNumChannels()) + 2] = (byte) (sample & 0xff);
					sampleBuffer[(i << flags.getInNumChannels()) + 3] = (byte) ((sample >> 8) & 0xff);
				}
			}
		}
		return (iread > 0);
	}
	
	public void close() {
		lame.close();
	}
	
	public int getChannels() {
		return lame.getFlags().getInNumChannels();
	}
	
	public int getSampleRate() {
		return lame.getFlags().getInSampleRate();
	}
	
	public int getFrameSize() {
		return lame.getFlags().getFrameSize();
	}
	
	public int getBufferSize() {
		return getChannels() * getFrameSize() * 2;
	}
}
