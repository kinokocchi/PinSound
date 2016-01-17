package info.pinlab.pinsound;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class WavClipTest {

	static String [] wavNames = new String[]{"sample.wav", "longsample.wav", "verylongsample.wav"}; 
	static List<WavClip> wavs = new ArrayList<WavClip>();


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
		assertTrue("Can't load wav file '" + wavName +"'", is!=null);
		return is;
	}

	

	@Test 
	public void testDblArray() throws Exception{
		int i =0;
		for(WavClip wav : wavs){
			double [] samples = wav.toDoubleArray();
			assertTrue(samples.length > 0);
			System.out.println("  Testing : " + wavNames[i]);
			assertTrue(samples.length > 0);
			
			assertTrue(samples.length > 0);
			double min = samples[0];
			double max = samples[0];
			double sum = 0;
			
			for(double sample : samples){
				sum += sample;
				min = sample < min ? sample : min;
				max = sample > max ? sample : max;
				//			System.out.println(d);
//				assertTrue("Negative value in sample!", d >= 0 );
//				assertTrue("Value > 1.0 in sample! '" + d +"'", d  <= 1.0 );
			}
			double mean = Math.abs(sum / (double)samples.length);		
			
			System.out.println("    Min = " + min);
			System.out.println("    Mean= " + mean);
			System.out.println("    Max = " + max);
			
			assertTrue("Min is less than -1.0! " + min , min >=-1.0);
			assertTrue("Max is more than  1.0! " + min , min <= 1.0);
			assertTrue("Non-zero sample mean for '" + wavNames[i] + "' (" + mean +")"
					, mean < 0.01); // <- this factor 
			i++;
		} //-- foreach .wav
	}

	
	
	@Test
	public void testIntArray() throws Exception{
		for(WavClip wav : wavs){
			int [] samples = wav.toIntArray();
//			System.out.println( samples.length);
			assertTrue(samples.length > 0);
			
			for(int i = 0 ; i< 10 ; i++){
//				System.out.println(samples[i]);
			}
			
		}
	}

}
