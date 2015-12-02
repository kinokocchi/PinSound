package info.pinlab.pinsound;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class WavClipTest {

	static String [] wavNames = new String[]{"sample.wav" /* , "longsample.wav", "verylongsample.wav" */ }; 
	static List<WavClip> wavs = new ArrayList<WavClip>();
	static int intForKid ;

	static {
		try{
			for(String wavName: wavNames) {
				InputStream is = getSampleWav(wavName);
				WavClip wav = new WavClip(is);
				assertTrue(wav != null);
				wavs.add(wav);
			}
			
			
			
		}catch(IOException e){
			assert(e==null);  //-- wow, this is ugly
		}catch(UnsupportedAudioFileException e){
			assert(e==null);  //-- wow, this is ugly
		}
	}

	static public InputStream getSampleWav(String wavName){
		InputStream is = WavClipTest.class.getResourceAsStream(wavName);
		if(is==null){
			WavClipTest.class.getClassLoader().getResourceAsStream(wavName);
		}
//		if(is==null){
//			getClass().getResourceAsStream(wavName);
//		}
//		if(is==null){
//			getClass().getResource(wavName);
//		}
		return is;
	}


	@Test
	public void test() throws Exception{
		//		assertTrue(getSampleWav()!=null);
		//		WavClip wav = new WavClip("/media/Doc/eclipse-projects/pinsound-wp/pinsound/audiocore/src/test/resources/info.pinlab.pinsound/sample.wav");
		//		System.out.println(wav.toWavFile());
	}


//	@Test 
	public void testDblArray() throws Exception{
		for(WavClip wav : wavs){
			double [] samples = wav.toDoubleArray();
			assertTrue(samples.length > 0);
			System.out.println(samples.length);
			assertTrue(samples.length > 0);
			for(double d : samples){
				//			System.out.println(d);
				assertTrue("Negative value in sample!", d >= 0 );
				assertTrue("Value > 1.0 in sample! '" + d +"'", d  <= 1.0 );
			}
		}
	}


	@Test
	public void testIntArray() throws Exception{
		for(WavClip wav : wavs){
			int [] samples = wav.toIntArray();
			System.out.println(samples.length);
			assertTrue(samples.length > 0);
			
			for(int i = 0 ; i< 10 ; i++){
				System.out.println(samples[i]);
			}
			
		}
	}

}
