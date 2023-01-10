/*
 *      Command line parsing related functions
 *
 *      Copyright (c) 1999 Mark Taylor
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/* $Id: Parse.java,v 1.33 2012/03/23 10:02:29 kenchis Exp $ */

package net.sourceforge.lame.mp3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

public class Parse {
	/**
	 * force byte swapping default=0
	 */
	public boolean swapbytes = false;
	/**
	 * Verbosity
	 */
	public int silent;
	public boolean embedded;
	public boolean brhist;
	/**
	 * to use Frank's time status display
	 */
	public float update_interval;
	/**
	 * to adjust the number of samples truncated during decode
	 */
	public int mp3_delay;
	/**
	 * user specified the value of the mp3 encoder delay to assume for decoding
	 */
	public boolean mp3_delay_set;
	public boolean disable_wav_header;
	/**
	 * print info whether waveform clips
	 */
	public boolean print_clipping_info;
	/**
	 * WAV signed
	 */
	public boolean in_signed = true;
	public ByteOrder in_endian = ByteOrder.LITTLE_ENDIAN;
	public int in_bitwidth = 16;
	ID3Tag id3;
	Presets pre;
	private Version version = new Version();
	/**
	 * Input: sound file format.
	 */
	private SoundFileFormat inputFormat;
	/**
	 * Ignore errors in values passed for tags
	 */
	private boolean ignore_tag_errors;
	/**
	 * Input: mp3InputData used by MP3
	 */
	private MP3Data mp3InputData = new MP3Data();
	
	public final void setModules(ID3Tag id32, Presets pre2) {
		this.id3 = id32;
		this.pre = pre2;
	}
	
	/**
	 * Input: Get sound file format.
	 *
	 * @return sound file format.
	 */
	public SoundFileFormat getInputFormat() {
		return inputFormat;
	}
	
	/**
	 * Input: Set sound file format.
	 *
	 * @param inputFormat sound file format.
	 */
	public void setInputFormat(SoundFileFormat inputFormat) {
		this.inputFormat = inputFormat;
	}
	
	public void initializeInputFormat(String filename) {
		this.setInputFormat(filenameToSoundFormat(filename));
	}
	
	/**
	 * Input: Get used by MP3.
	 *
	 * @return the mp3InputData used by MP3
	 */
	public MP3Data getMp3InputData() {
		return mp3InputData;
	}
	
	/**
	 * Input: Set mp3InputData used by MP3.
	 *
	 * @param mp3InputData the mp3InputData mp3InputData
	 */
	public void setMp3InputData(MP3Data mp3InputData) {
		this.mp3InputData = mp3InputData;
	}
	
	private boolean set_id3tag(final LameGlobalFlags gfp, final int type, final String str) {
		switch(type) {
		case 'a':
			id3.id3tag_set_artist(gfp, str);
			return false;
		case 't':
			id3.id3tag_set_title(gfp, str);
			return false;
		case 'l':
			id3.id3tag_set_album(gfp, str);
			return false;
		case 'g':
			id3.id3tag_set_genre(gfp, str);
			return false;
		case 'c':
			id3.id3tag_set_comment(gfp, str);
			return false;
		case 'n':
			id3.id3tag_set_track(gfp, str);
			return false;
		case 'y':
			id3.id3tag_set_year(gfp, str);
			return false;
		case 'v':
			id3.id3tag_set_fieldvalue(gfp, str);
			return false;
		}
		return false;
	}
	
	private boolean set_id3v2tag(final LameGlobalFlags gfp, final int type, final String str) {
		switch(type) {
		case 'a':
			id3.id3tag_set_textinfo_ucs2(gfp, "TPE1", str);
			return false;
		case 't':
			id3.id3tag_set_textinfo_ucs2(gfp, "TIT2", str);
			return false;
		case 'l':
			id3.id3tag_set_textinfo_ucs2(gfp, "TALB", str);
			return false;
		case 'g':
			id3.id3tag_set_textinfo_ucs2(gfp, "TCON", str);
			return false;
		case 'c':
			id3.id3tag_set_comment(gfp, null, null, str, 0);
			return false;
		case 'n':
			id3.id3tag_set_textinfo_ucs2(gfp, "TRCK", str);
			return false;
		}
		return false;
	}
	
	private boolean id3_tag(final LameGlobalFlags gfp, final int type, final TextEncoding enc, final String str) {
		String x = null;
		boolean result;
		switch(enc) {
		default:
		case TENC_RAW:
			x = str;
			break;
		case TENC_LATIN1:
			x = str/* toLatin1(str) */;
			break;
		case TENC_UCS2:
			x = str/* toUcs2(str) */;
			break;
		}
		switch(enc) {
		default:
		case TENC_RAW:
		case TENC_LATIN1:
			result = set_id3tag(gfp, type, x);
			break;
		case TENC_UCS2:
			result = set_id3v2tag(gfp, type, x);
			break;
		}
		return result;
	}
	
	private int presets_set(final LameGlobalFlags gfp, final int fast, final int cbr, String preset_name, final String ProgramName) {
		int mono = 0;
		
		if((preset_name.equals("help")) && (fast < 1) && (cbr < 1)) {
			System.out.println(version.getVersion());
			System.out.println();
			return -1;
		}
		
		/* aliases for compatibility with old presets */
		
		if(preset_name.equals("phone")) {
			preset_name = "16";
			mono = 1;
		}
		if((preset_name.equals("phon+")) || (preset_name.equals("lw")) || (preset_name.equals("mw-eu")) || (preset_name.equals("sw"))) {
			preset_name = "24";
			mono = 1;
		}
		if(preset_name.equals("mw-us")) {
			preset_name = "40";
			mono = 1;
		}
		if(preset_name.equals("voice")) {
			preset_name = "56";
			mono = 1;
		}
		if(preset_name.equals("fm")) {
			preset_name = "112";
		}
		if((preset_name.equals("radio")) || (preset_name.equals("tape"))) {
			preset_name = "112";
		}
		if(preset_name.equals("hifi")) {
			preset_name = "160";
		}
		if(preset_name.equals("cd")) {
			preset_name = "192";
		}
		if(preset_name.equals("studio")) {
			preset_name = "256";
		}
		
		if(preset_name.equals("medium")) {
			pre.lame_set_VBR_q(gfp, 4);
			if(fast > 0) {
				gfp.setVBR(VbrMode.vbr_mtrh);
			} else {
				gfp.setVBR(VbrMode.vbr_rh);
			}
			return 0;
		}
		
		if(preset_name.equals("standard")) {
			pre.lame_set_VBR_q(gfp, 2);
			if(fast > 0) {
				gfp.setVBR(VbrMode.vbr_mtrh);
			} else {
				gfp.setVBR(VbrMode.vbr_rh);
			}
			return 0;
		} else if(preset_name.equals("extreme")) {
			pre.lame_set_VBR_q(gfp, 0);
			if(fast > 0) {
				gfp.setVBR(VbrMode.vbr_mtrh);
			} else {
				gfp.setVBR(VbrMode.vbr_rh);
			}
			return 0;
		} else if((preset_name.equals("insane")) && (fast < 1)) {
			
			gfp.preset = Lame.INSANE;
			pre.apply_preset(gfp, Lame.INSANE, 1);
			
			return 0;
		}
		
		/* Generic ABR Preset */
		if(((Integer.valueOf(preset_name)) > 0) && (fast < 1)) {
			if((Integer.valueOf(preset_name)) >= 8 && (Integer.valueOf(preset_name)) <= 320) {
				gfp.preset = Integer.valueOf(preset_name);
				pre.apply_preset(gfp, Integer.valueOf(preset_name), 1);
				
				if(cbr == 1)
					gfp.setVBR(VbrMode.vbr_off);
				
				if(mono == 1) {
					gfp.setMode(MPEGMode.MONO);
				}
				
				return 0;
				
			} else {
				System.err.println(version.getVersion());
				System.err.println();
				System.err.printf("Error: The bitrate specified is out of the valid range for this preset\n" + "\n" + "When using this mode you must enter a value between \"32\" and \"320\"\n" + "\n" + "For further information try: \"%s --preset help\"\n", ProgramName);
				return -1;
			}
		}
		
		System.err.println(version.getVersion());
		System.err.println();
		System.err.printf("Error: You did not enter a valid profile and/or options with --preset\n" + "\n" + "Available profiles are:\n" + "\n" + "   <fast>        medium\n" + "   <fast>        standard\n" + "   <fast>        extreme\n" + "                 insane\n" + "          <cbr> (ABR Mode) - The ABR Mode is implied. To use it,\n" + "                             simply specify a bitrate. For example:\n" + "                             \"--preset 185\" activates this\n" + "                             preset and uses 185 as an average kbps.\n" + "\n");
		System.err.printf("    Some examples:\n" + "\n" + " or \"%s --preset fast standard <input file> <output file>\"\n" + " or \"%s --preset cbr 192 <input file> <output file>\"\n" + " or \"%s --preset 172 <input file> <output file>\"\n" + " or \"%s --preset extreme <input file> <output file>\"\n" + "\n" + "For further information try: \"%s --preset help\"\n", ProgramName, ProgramName, ProgramName, ProgramName, ProgramName);
		return -1;
	}
	
	/**
	 * LAME is a simple frontend which just uses the file extension to determine
	 * the file type. Trying to analyze the file contents is well beyond the
	 * scope of LAME and should not be added.
	 */
	private SoundFileFormat filenameToSoundFormat(String filename) {
		int len = filename.length();
		if(len < 4) {
			return SoundFileFormat.UNKNOWN;
		}
		switch(filename.substring(len - 4).toLowerCase()) {
		case ".mpg":
		case ".mp1":
		case ".mp2":
		case ".mp3":
			return SoundFileFormat.MP123;
		case ".wav":
			return SoundFileFormat.WAVE;
		case ".aif":
			return SoundFileFormat.AIFF;
		case ".raw":
			return SoundFileFormat.RAW;
		default:
			return SoundFileFormat.UNKNOWN;
		}
	}
	
	private int resample_rate(double freq) {
		if(freq >= 1.e3)
			freq *= 1.e-3;
		
		switch((int) freq) {
		case 8:
			return 8000;
		case 11:
			return 11025;
		case 12:
			return 12000;
		case 16:
			return 16000;
		case 22:
			return 22050;
		case 24:
			return 24000;
		case 32:
			return 32000;
		case 44:
			return 44100;
		case 48:
			return 48000;
		default:
			System.err.printf("Illegal resample frequency: %.3f kHz\n", freq);
			return 0;
		}
	}
	
	private int set_id3_albumart(final LameGlobalFlags gfp, final String file_name) {
		int ret = -1;
		RandomAccessFile fpi = null;
		
		if(file_name == null) {
			return 0;
		}
		try {
			fpi = new RandomAccessFile(file_name, "r");
			try {
				int size = (int) (fpi.length() & Integer.MAX_VALUE);
				byte[] albumart = new byte[size];
				fpi.readFully(albumart);
				ret = id3.id3tag_set_albumart(gfp, albumart, size) ? 0 : 4;
			} catch(IOException e) {
				ret = 3;
			} finally {
				try {
					fpi.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		} catch(FileNotFoundException e1) {
			ret = 1;
		}
		switch(ret) {
		case 1:
			System.err.printf("Could not find: '%s'.\n", file_name);
			break;
		case 2:
			System.err.printf("Insufficient memory for reading the albumart.\n");
			break;
		case 3:
			System.err.printf("Read error: '%s'.\n", file_name);
			break;
		case 4:
			System.err.printf("Unsupported image: '%s'.\nSpecify JPEG/PNG/GIF image (128KB maximum)\n", file_name);
			break;
		default:
			break;
		}
		return ret;
	}
	
	/**
	 * possible text encodings
	 */
	private enum TextEncoding {
		/**
		 * bytes will be stored as-is into ID3 tags, which are Latin1/UCS2 per
		 * definition
		 */
		TENC_RAW,
		/**
		* text will be converted from local encoding to Latin1, as
		* ID3 needs it
		*/
		TENC_LATIN1,
		/**
		* text will be converted from local encoding to UCS-2, as
		* ID3v2 wants it
		*/
		TENC_UCS2
	}
	
	private enum ID3TAG_MODE {
		ID3TAG_MODE_DEFAULT, ID3TAG_MODE_V1_ONLY, ID3TAG_MODE_V2_ONLY
	}
	
	public static class NoGap {
		int num_nogap;
	}
	
}
