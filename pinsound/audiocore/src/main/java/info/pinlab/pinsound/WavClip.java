package info.pinlab.pinsound;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.codec.binary.Base64;


/**
 * Holds samples of a wav file in a bytearray.
 * 
 * @author Gabor Pinter
 *
 */
public class WavClip implements Serializable, AudioClipIF{
	private static final long serialVersionUID = 4431714308845293L;
	byte samples[];
	int samplingRate;
	int channelN;
	int bit;
	boolean isBigEnd;
	String name = null;
	transient AudioFormat af ;

	//-- some stats
//	Integer sampleMin = null;
//	Integer sampleMax = null;

	
	static MessageDigest sha256Digest ;
	static {
		try {
			sha256Digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	

	/*
	 * Instructor using a InputStream to a PCM wave file.
	 */
	public WavClip (InputStream is) throws IOException, UnsupportedAudioFileException{
		if(is == null)
			throw new IllegalArgumentException("InputStreaim is null!");

		//-- new BufferedInputStream is needed because 
		//-- it causes the following exception with openJDK (but not with Sun's):
		//-- 
		//  Exception in thread "main" java.io.IOException: mark/reset not supported
		//   at java.util.zip.InflaterInputStream.reset(InflaterInputStream.java:286)
		//   at java.io.FilterInputStream.reset(FilterInputStream.java:217)
		//   at com.sun.media.sound.SoftMidiAudioFileReader.getAudioInputStream(SoftMidiAudioFileReader.java:135)
		//   at javax.sound.sampled.AudioSystem.getAudioInputStream(AudioSystem.java:1111)

		AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
		streamToBytes(ais);
		name = "";
	}


	public WavClip(ByteArrayOutputStream baos, AudioFormat af){
		samples = baos.toByteArray();

		this.af = af;
		samplingRate = Math.round(af.getFrameRate());
		channelN = af.getChannels();
		bit = af.getSampleSizeInBits();
		isBigEnd = af.isBigEndian();
	}

	/*
	 * Instructor using a path to a PCM wave file.
	 */
	public WavClip (String pathToWav) throws IOException{
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(pathToWav));
			streamToBytes(ais);
			name = new File(pathToWav).getName();
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
		}
	}

	public WavClip (AudioInputStream ais){
		name = "--on-the-fly--";
		streamToBytes(ais);
	}


	/**
	 * Copies interval into a new WavClip object.
	 * 
	 * @param startT selection start time in milliseconds
	 * @param endT selection end time in milliseconds
	 * @return the selected interval as {@link WavClip} 
	 */
	public WavClip getSelectionByTime(long startT, long endT){
		float bytePerMs = getBytePerMs();
		int startFrameX = (int)Math.floor(startT * bytePerMs);
		int endFrameX = (int)Math.floor(endT * bytePerMs);

		endFrameX = (endFrameX>samples.length) ? samples.length : endFrameX;
		//		System.out.println(startFrameX + "-" + endFrameX + "    (len " + (samples.length) + ") " + bytePerMs);

		int len = endFrameX-startFrameX /*involve last frame*/; 

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(samples, startFrameX, len);
		return new WavClip(baos, af);
	}

	private void streamToBytes(AudioInputStream ais) {
		try{
			samples = new byte[ais.available()];
			//			System.out.println("Available : " + ais.available());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			int buffN = 1024*8;
			byte[] buff = new byte[buffN];
			int read = 0;
			while(true){
				read = ais.read(buff, 0, buffN);
				if(read < 0)
					break;
				baos.write(buff, 0, read);
			}
			baos.close();
			samples = baos.toByteArray();

			af = ais.getFormat();
			samplingRate = Math.round(af.getFrameRate());
			channelN = af.getChannels();
			bit = af.getSampleSizeInBits();
			isBigEnd = af.isBigEndian();
			//	        System.out.println(af);
		}catch(IOException e){};
	}


	/**
	 * 
	 * @return the number of bytes used to encode 1ms of audio
	 */
	public float getBytePerMs(){
		float bitPerSec = af.getFrameRate()*af.getFrameSize()*af.getChannels();
		return bitPerSec/1000;
	}


	//	@Override
	public long getDurInMs(){
		float bitPerSec = af.getFrameRate()*af.getFrameSize()*af.getChannels();
		//		System.out.println((int)Math.floor(1000*(samples.length / bitPerSec)));
		return (long)Math.floor(1000*(samples.length / bitPerSec));
	}

	//	@Override
	public byte[] getSamples() {
		return samples;
	}


	public int getDurInFrames(){
		return samples.length / (af.getFrameSize()*af.getChannels());
	}


	public AudioFormat getAudioFormat(){
		return af != null ? af :  
			new AudioFormat(samplingRate, bit, channelN, true, isBigEnd) ;
	}

	public AudioInputStream getAudioInputStream(){
		return new AudioInputStream(new ByteArrayInputStream(samples),
				getAudioFormat(),
				samples.length);
	}

	
	
	/**
	 * 
	 * @return normalized double array of samples with range {-1,1}
	 */
	public double[] toDoubleArray(){
		int [] samplesAsInt = toIntArray();
		double [] samplesAsDbl = new double[samplesAsInt.length];

		double div = 0;
		int pad = 0;
		
		switch(af.getSampleSizeInBits()){
		case(8):
			div = 256.0d;
			break;
		case(16):
			div = 65536;
			break;
		default:
			throw new IllegalStateException("Can't handle files with " + af.getSampleSizeInBits() +" bit depth!");
		};
		
		if(AudioFormat.Encoding.PCM_SIGNED.equals(af.getEncoding())){
			pad = 0;// (int)div/2;
		}else{
			throw new IllegalStateException("Can't handle files with " + af.getSampleSizeInBits() +" bit depth!");
		}
				
		for(int i = 0; i < samplesAsInt.length ; i++){
			samplesAsDbl[i] = (samplesAsInt[i]+pad )/div;
		}
		
		return samplesAsDbl;
	}
	
	
	
	private int sampleN = 0;
	private long sampleSum = 0;
	private int sampleMin = Integer.MAX_VALUE;
	private int sampleMax = Integer.MIN_VALUE;
	double sampleMean = 0;
//	int sampleN
	
	/**
	 * 
	 * @return int array of samples.
	 */
	public int[] toIntArray(){
		boolean isMono = (af.getChannels() == 1);
		if(!isMono){
			throw new IllegalArgumentException("Can handle only mono sound! This sound has " + af.getChannels() +" channels!");
		}
		byte[] bytes = this.getSamples();
		int bytePerFrame = af.getSampleSizeInBits() / 8; /* * this.getAudioFormat().getChannels(); */
		sampleN = bytes.length/bytePerFrame;
		
		int [] samples = new int[bytes.length/bytePerFrame];
		int j = 0;
		
		
		if(bytePerFrame == 1){ 
			for (int i = 0 ; i < bytes.length ; i += bytePerFrame){
				int sampleAsInt = 0;
				sampleAsInt = (int) bytes[i];
				samples[j++] = sampleAsInt;
				//-- stats
				sampleSum += sampleAsInt;
				sampleMin  = sampleAsInt < sampleMin ? sampleAsInt : sampleMin;
				sampleMax  = sampleAsInt > sampleMax ? sampleAsInt : sampleMax;
			}
		}
		
		if(bytePerFrame == 2){ 
			if(!af.isBigEndian() /* LittleEndian */ ){
				for (int i = 0 ; i < bytes.length ; i += bytePerFrame){
					int sampleAsInt = 0;
//					System.out.println("LE -> Int");
					//-- Little Endian to signed integer --//
					sampleAsInt = (int) bytes[i+1];
					sampleAsInt <<= 8;
					sampleAsInt |= bytes[i] & 0x00FF  ;
					samples[j++] = sampleAsInt;
					//-- stats
					sampleSum += sampleAsInt;
					sampleMin  = sampleAsInt < sampleMin ? sampleAsInt : sampleMin;
					sampleMax  = sampleAsInt > sampleMax ? sampleAsInt : sampleMax;
				}
			}else{ //-- not big endian
				for (int i = 0 ; i < bytes.length ; i += bytePerFrame){
					int sampleAsInt = 0;
	//				System.out.println("BE -> Int");
					sampleAsInt = (int) bytes[i];
					sampleAsInt <<= 8;
					sampleAsInt |= bytes[i+1] & 0x00FF  ;
					samples[j++] = sampleAsInt;
					//-- stats
					sampleSum += sampleAsInt;
					sampleMin  = sampleAsInt < sampleMin ? sampleAsInt : sampleMin;
					sampleMax  = sampleAsInt > sampleMax ? sampleAsInt : sampleMax;
				}
			}
		}
		
		
		for (int i = 0 ; i < bytes.length ; i += bytePerFrame){
			int sampleAsInt = 0;
			if(bytePerFrame == 1){ 
				sampleAsInt = (int) bytes[i];
			}else
			if(bytePerFrame == 2){ 
				if(!af.isBigEndian() /* LittleEndian */ ){
//					System.out.println("LE -> Int");
					//-- Little Endian to signed integer --//
					sampleAsInt = (int) bytes[i+1];
					sampleAsInt <<= 8;
					sampleAsInt |= bytes[i] & 0x00FF  ;
				}else{
					//-- Big Endian to signed integer --//
//					System.out.println("BE -> Int");
					sampleAsInt = (int) bytes[i];
					sampleAsInt <<= 8;
					sampleAsInt |= bytes[i+1] & 0x00FF  ;
				}
			}
			samples[j++] = sampleAsInt;
			//-- stats
			sampleSum += sampleAsInt;
			sampleMin  = sampleAsInt < sampleMin ? sampleAsInt : sampleMin;
			sampleMax  = sampleAsInt > sampleMax ? sampleAsInt : sampleMax;
		}
		sampleMean += 1.0*sampleSum/sampleN;  
		bytes = null;
		return samples;
	}


	/**
	 * Returns stats about samples. 
	 * 
	 * @return min, max, sum, n
	 */
	public long[] getSampleMinMaxSum(){
		return new long[]{sampleMin, sampleMax, sampleSum, sampleN};
	}
	
	
	public String getName(){
		return name;
	}

	@Override
	public String toString(){
		String hz ;
		switch(samplingRate){
		case 8000:
			hz = "8kHz ";
			break;
		case 11025:
			hz = "11kHz ";
			break;
		case 16000:
			hz = "16kHz ";
			break;
		case 22050:
			hz = "16kHz ";
			break;
		case 32000:
			hz = "32kHz ";
			break;
		case 44100:
			hz = "44.1kHz ";
			break;
		default : 
			hz = String.format("%2.2fkHz ", Math.round(samplingRate / 10.0f)/100.0f);
		}

		return  hz 
				+ channelN +"ch " 
				+ bit + "bit "
				+ (isBigEnd ? "be" : "le ") 
				+ getDurInMs() + "ms"
				;
		//		44.1kHz 1ch 16bit le
	}


	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof WavClip))
			return false;
		WavClip other = (WavClip) obj;
		if (this.samples.length != other.samples.length)
			return false;
		if (this.hashCode() != other.hashCode())
			return false;
		for(int i = 0 ; i < this.samples.length ; i++)
			if(this.samples[i] != other.samples[i])
				return false;
		return true;
	}

	Integer hash = null;

	@Override
	public int hashCode(){
		if (hash != null)
			return hash;
		//-- calculate if necessary --//
		//TODO: put this back
		hash = getSHA256(this.samples).hashCode();
		return hash;
	}


	public String toLongString(){
		return 	af.getEncoding() + " " + 
				samplingRate + "Hz " + 
				bit + "bit " +
				(channelN == 1 ? "mono " : "stereo ") + 
				(isBigEnd == true ? "be " : "le ")+
				"(" + samples.length + " byte)"
				;
	}


	
	public static String getSHA256(byte [] raw){
		final byte[] digested = sha256Digest.digest(raw);
		String sha = null;
		try {
			sha = new String(Base64.encodeBase64(digested), "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sha;
	}

	/**
	 * 
	 * Use this to write wav to file! 
	 * 
	 * 
	 * <pre>
	 * {@code
	 * FileOutputStream fos = new FileOutputStream(savePath);
	 * fos.write(wav.toWavFile());
	 * fos.close();
	 * }
	 * </pre>
	 * 
	 * @return a byte array - header included!
	 */
	public byte[] toWavFile() {
		int sub2ChSz = samples.length; //(int) Math.floor(samples.length * af.getSampleSizeInBits()/8.0f);
		byte[] sz2 = new byte[]{
				(byte) (sub2ChSz & 0x00ff ),
				(byte) (sub2ChSz >> 8 & 0x00ff),
				(byte) (sub2ChSz >> 16 & 0x00ff),
				(byte) (sub2ChSz >>> 24)
		};

		int sub1ChSz = 36 + sub2ChSz; 
		byte [] sz = new byte[]{
				(byte) (sub1ChSz & 0x00ff ),
				(byte) (sub1ChSz >> 8 & 0x00ff),
				(byte) (sub1ChSz >> 16 & 0x00ff),
				(byte) (sub1ChSz >>> 24)
		};

		byte chN = (byte) (af.getChannels()  & 0x00ff);

		int hzInt = (int)Math.floor(af.getSampleRate());
		byte[] hz = new byte[]{
				(byte) (hzInt & 0x00ff ),
				(byte) (hzInt >> 8 & 0x00ff),
				(byte) (hzInt >> 16 & 0x00ff),
				(byte) (hzInt >>> 24)
		};
		//		System.out.println(hzInt + " " + Integer.toHexString(hzInt));
		//		for(byte b : hz)
		//			System.out.println("\t"+ Integer.toHexString(b));

		int byteRateInt = (int)Math.floor(af.getSampleRate() * af.getChannels()* af.getSampleSizeInBits()/8.0f);
		byte[] br = new byte[]{
				(byte) (byteRateInt & 0x00ff ),
				(byte) (byteRateInt >> 8 & 0x00ff),
				(byte) (byteRateInt >> 16 & 0x00ff),
				(byte) (byteRateInt >>> 24)
		};

		int blockAlignInt = (int)Math.floor(af.getChannels()* af.getSampleSizeInBits()/8.0f);
		byte[] ba = new byte[]{
				(byte) (blockAlignInt & 0x00ff ),
				(byte) (blockAlignInt >> 8 & 0x00ff),
		};

		int bitsPerSampleInt = af.getSampleSizeInBits();
		byte[] bps = new byte[]{
				(byte) (bitsPerSampleInt & 0x00ff ),
				(byte) (bitsPerSampleInt >> 8 & 0x00ff),
		};

		//REFERENCE]		https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
		byte[] header = new byte[]{
				/*  R     I     F     F         size                          W     A     W     E    */
				0x52, 0x49, 0x46, 0x46,     sz[0],sz[1],sz[2],sz[3],      0x57, 0x41, 0x56, 0x45,
				/*  f     m     t     <spc>     /Subchunk1Size = 16 /         /AudioFormat=1(PCM)/    /#of channels/     */ 
				0x66, 0x6d, 0x74, 0x20,     0x10, 0x00, 0x00, 0x00,       0x01, 0x00,             chN, 0x00,
				/* SamplingRate                 Byte Rate                     Block Align             BitPerSample       */
				hz[0],hz[1],hz[2],hz[3],    br[0],br[1],br[2],br[3],      ba[0],ba[1],            bps[0],bps[1],  
				/*  d     a     t     a         size2                         ... data... */
				0x64, 0x61, 0x74, 0x61,     sz2[0],sz2[1],sz2[2],sz2[3]  
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try{
			baos.write(header);
			baos.write(samples);
		}catch(IOException e){
			System.err.println("IOException  " + e);
			//			logger.error();
		}
		return baos.toByteArray();
	}


	public static WavClip add(WavClip wav1, WavClip wav2){
		AudioFormat af = wav1.getAudioFormat();
		if(af.equals(wav2.getAudioFormat())){
			throw new IllegalArgumentException("AudioFormat not compatible for the two wav files!\n" +
					"wav1 : '" + wav1.getAudioFormat() + "'" +
					"wav2 : '" + wav2.getAudioFormat() + "'" );
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(wav1.getSamples());
			baos.write(wav2.getSamples());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new WavClip(baos, af);
	}


	public static void main(String[] args) throws Exception{
		String sample = "/home/kinoko/Desktop/wavclip-test.wav";
		WavClip wc = new WavClip(sample);
		
		int [] array = wc.toIntArray();
		for (int i : array){
			System.out.println(i);
		}
		
		
//		System.out.println(wc.getAudioFormat().getEncoding());
//		System.out.println(wc);
//		byte [] bytes = wc.toWavFile();
//
//		FileOutputStream fos = new FileOutputStream("data/snd/sample-copy.wav");
//		fos.write(bytes);
//		fos.close();
//		byte b1 = (byte)0x7F;
//		byte b2 = (byte)0x80;
//		System.out.println(Integer.toHexString(b1));
//		System.out.println(Integer.toHexString(b2));
//
//		//		 FileOutputStream fis = new FileOutputStream("wav.obj");
//		//		 ObjectOutputStream ois  = new ObjectOutputStream(fis);
//		//		 ois.writeObject(wc);
//		//		 fis.close();
//
	}

}



