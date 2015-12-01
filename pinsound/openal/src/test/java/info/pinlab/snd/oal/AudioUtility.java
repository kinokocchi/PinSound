package info.pinlab.snd.oal;

import static org.junit.Assert.assertTrue;
import info.pinlab.pinsound.WavClip;

import java.io.InputStream;

public class AudioUtility {

	
	public static WavClip loadWav(String wavName) throws Exception{
		InputStream is = AudioUtility.class.getResourceAsStream(wavName);
		if(is==null){ //-- try different loading
			is = AudioUtility.class.getClassLoader().getResourceAsStream(wavName);
		}
		assertTrue(is!=null);
		WavClip wav = new WavClip(is);
		return wav;
	}
	
}
