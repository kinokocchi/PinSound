package info.pinlab.snd.oal;

import org.lwjgl.openal.AL10;

public enum RecFormat {
	MONO8_8k(AL10.AL_FORMAT_MONO8, 8000, 1),
	MONO16_8k(AL10.AL_FORMAT_MONO16, 8000, 2),
	MONO8_16k(AL10.AL_FORMAT_MONO8, 16000, 1),
	MONO16_16k(AL10.AL_FORMAT_MONO16, 16000, 2),
	;
	final int alFormat;
	final int hz;
	final int recBytePerSample;

	RecFormat(int format, int hz, int bytePerSample){
		this.alFormat =format;
		this.hz = hz;
		this.recBytePerSample = bytePerSample;
	}
	public String toString(){
		return hz+"Hz " + recBytePerSample*8 +"b " + "mono";
	}
	
	static public RecFormat getAudioFormat(int ch, int hz, int bit){
		if(ch==1){
			switch (hz) {
			case 8000:
				switch (bit) {
				case 8:
					return MONO8_8k;
				case 16:
					return MONO8_16k;
				default:
					break;
				}
			case 16000:
				switch (bit) {
				case 8:
					return MONO16_8k;
				case 16:
					return MONO16_16k;
				default:
					break;
				}
			default:
				break;
			}
		}
		throw new IllegalArgumentException("No such audio format! " + ch +"ch " + hz +"hz " + bit +"b");
	}
}
