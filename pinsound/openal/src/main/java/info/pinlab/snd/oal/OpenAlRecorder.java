package info.pinlab.snd.oal;


import info.pinlab.pinsound.RecorderDeviceController;
import info.pinlab.pinsound.WavClip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.IllegalSelectorException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCdevice;


/**
 * <pre>
 * {@code
public class AudioRecWorker extends SwingWorker<WavClip, Void> {
	private WavClip wav = null;
	@Override
	protected WavClip doInBackground(){
		try{
			recorder.rec();
			wav = recorder.getWavClip();
		}catch(IllegalStateException e){
			logger.error("Cant retrieve audio " + e);
			final AppActionEvent err = new AppActionEvent(this, AppAction.ERR_SND_REC);
			err.setParam(Param.MSG, e.getMessage());
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
			   // ... HANDLE ERROR ... //
					}
				});
			}
			return wav;
		}
		//-- called from different thread
		void stop(){
			recorder.stop();
		}
		
		@Override
		protected void done() {
			if(wav!=null){
				//-- do sg with the sound  --//
			}
		}
	}
}
 * </pre>
 */
public class OpenAlRecorder implements RecorderDeviceController {
	private static final Logger logger = LoggerFactory.getLogger(OpenAlRecorder.class);

	private WavClip recordedWav = null;
	static float recResolutionInSec = Sys.getTimerResolution()/1000.0f;
	public long AUDIO_MAX_SIZE_IN_KBYTE = 16*1024; //-- max 5MB per file 

	double maxAudioSzPerRecInSec = 0;
	long recBuffStorageSizeInKb = 0;
	int maxSndSizeInKb = 1024;

	static String [] recDevList = null;
	
	volatile private boolean isRecInterruptReq = false; 

	private static String defaultRecDeviceName = null;
	private static final int DEVICE_UNSET   = -2;
	private static final int DEVICE_DEFAULT = -1;
	private static int defaultRecDeviceIx = DEVICE_UNSET;
	
	private ALCdevice currentRecDev = null ;
	private int currentRecDevId = DEVICE_UNSET;
	private RecFormat rformat;
	
	private List<ByteBuffer> sampleBufferStorage = null;
	private int sampleBuffSzInSamples ;
	private int sampleBuffSzInBytes;
	private final IntBuffer availableSamples = BufferUtils.createIntBuffer(2);
	
	private ByteBuffer sampleBuff;
	private int sampleBuffBytePos = 0;
	private int avail = 0 ;
	int sampleBufferStorageSize = 1;
	
	
	
	
	private OpenAlRecorder(){
		logger.debug("OpenAlRecorder instance created");
	}
	private static OpenAlRecorder singleton = new OpenAlRecorder();
	
	
	public static OpenAlRecorder getInstance(){
		return singleton;
	}
	
	
	private boolean _createDefaultAL() throws IllegalSelectorException {
		try{
			if(!AL.isCreated()){
				logger.info("Creating native OpenAL resource");
				AL.create();
			}else{
				logger.debug("(Native OpenAL resource is already created)");
			}
	  }catch (LWJGLException le) {
		  for(StackTraceElement e : le.getStackTrace()){
				logger.error("Couldn't create native OpenAL resource");
				logger.error(e.getClassName() + "#" + e.getMethodName());
		  }
		  return false;
//		  throw new IllegalStateException("Couldn't create OpenAL player");
	  }
	  AL10.alGetError();
	  return true;
	}

	private void reInit(){
		isRecInterruptReq = false;
		sampleBuffBytePos = 0;
		avail = 0 ;
		sampleBufferStorage = new ArrayList<ByteBuffer>(sampleBufferStorageSize);
		sampleBuff = org.lwjgl.BufferUtils.createByteBuffer(sampleBuffSzInBytes);
		sampleBufferStorage.add(sampleBuff);
		recordedWav = null;
	}
	
	
	@Override
	synchronized public boolean isRecording(){
		return isRecording;
	}
	
	
	synchronized private void isRecording(boolean b){
		isRecording = b;
	}

	
	@Override
	public void setRecFormat(int chanelN, int hz, int bit)throws IllegalArgumentException{
		RecFormat recFormat = RecFormat.getAudioFormat(chanelN, hz, bit);
		init(recFormat);
	}
	
	
	private void init(RecFormat rformat) throws IllegalStateException{
		while(isRecording()){
			//-- wait till recording is finished --//
			try{
				logger.warn("Can't init while recording : retrying.. ");
				Thread.sleep(200);
			}catch(InterruptedException e){
				break;
			}
		}
		
		 logger.info("Initializing rec device " +  rformat);
		 if(this.rformat != null && this.rformat.equals(rformat)){
			 logger.info("Rec device is already initialized for this format! Skipping init!");
			 return;
		 }
		 this.rformat = rformat;
		_createDefaultAL(); 
		//-- Recording resolution : how often will samples be available (1sec is normal)
		//-- Sample buffer : to hold these samples
		 sampleBuffSzInSamples = (int)Math.floor(rformat.hz * recResolutionInSec * 1.2); 
		 sampleBuffSzInBytes = sampleBuffSzInSamples * rformat.recBytePerSample; //-- same
//		 maxAudioSzPerRecInKb  = AUDIO_MAX_SIZE_IN_SEC * rformat.hz * rformat.recBytePerSample / 1024.0f; 
		 maxAudioSzPerRecInSec = AUDIO_MAX_SIZE_IN_KBYTE * 1024.0f / (rformat.hz * rformat.recBytePerSample);
		 sampleBufferStorageSize = (int)Math.ceil((maxSndSizeInKb * 1024.0f) / (double )sampleBuffSzInBytes  );
		 
		 logger.info("|-Recording resolution  " +  recResolutionInSec + " second");
		 logger.info("|-Recording buffer size " + sampleBuffSzInSamples     + " samples");
		 logger.info("|-Recording buffer size " + sampleBuffSzInBytes     + " bytes");
//		 logger.info("Max recording size " + maxAudioSzPerRecInKb     + " kbytes");
		 logger.info("|-Max recording duration  " + maxAudioSzPerRecInSec     + " sec");
		 
		 logger.info("Opening default record device");
		 try{
			 //-- TODO: fails in LWJGL 2.9.1  --//
			 defaultRecDeviceName = ALC10.alcGetString(null, ALC11.ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER);
			 logger.debug("|-Default REC devica name '" + defaultRecDeviceName + "'");
		 }catch(Throwable e){
			 logger.error("FAILED to retrieve specifier string for the DEFAULT devices!");
			 logger.error("|- " + e.getMessage());
		 }
		 
		 //-- returns available capture device names separated by '0'
		 try{
			 //-- TODO: fails in LWJGL 2.9.1  --//
			 String recDevicesAsString = ALC10.alcGetString(null, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);
			 if(recDevicesAsString == null)
				 throw new IllegalStateException("Couldn't retrieve Rec Device names");
			 recDevList = recDevicesAsString.split("\0");
			 
			 for(int i = 0 ; i < recDevList.length ; i++){
//			 String ascii = new String(defaultRecDeviceName.getBytes(Charset.forName("Shift-JIS")), Charset.forName("ASCII"));
//			 currentRecDev = ALC11.alcCaptureOpenDevice(recDevList[i]/* deviceId */,rformat.hz, rformat.alFormat, sampleBuffSzInSamples /* buff */ );
				 String defaultMark = " ";
				 if ( defaultRecDeviceName!=null && recDevList[i].equals(defaultRecDeviceName)){
					 defaultMark = "*";
					 defaultRecDeviceIx = i;
				 }
				 logger.debug("|-Available audio CAPTURE device :" + defaultMark  + "[" + (i) + "] '" + recDevList[i] + "'");
			 }
			 if(defaultRecDeviceIx == DEVICE_UNSET){
				 logger.warn("Couldn't find default REC device by name!");
//			 defaultRecDeviceIx = DEVICE_DEFAULT;
			 }
			 //-- open capture device by id
			 setRecorderDevice(defaultRecDeviceIx);
		 }catch(Throwable e){
			 logger.error("FAILED to retrieve specifier string for ALL devices!");
			 logger.error("|- " + e.getMessage());
			 setRecorderDevice(DEVICE_DEFAULT);
		 }
//		 String defaultRecDeviceNameASCII = new String(defaultRecDeviceName.getBytes(Charset.forName("Shift-JIS")), Charset.forName("ASCII"));
//		 char[] chars1 = defaultRecDeviceName.toCharArray();
//		 byte[] bytes2 = defaultRecDeviceName.getBytes(Charset.forName("Shift-JIS"));
//		 char[] chars3 = defaultRecDeviceNameASCII.toCharArray();
//		 for(int i = 0 ; i < chars1.length ; i++){
//			 Character c = new Character(chars1[i]);
//			 System.out.println(Character.getNumericValue(c));
//			 System.out.println(chars1[i] + "\t" + bytes2[i] + "\t" + (int)chars3[i]);
//			 System.out.println(Integer.toBinaryString(chars1[i]) + "\t" + Integer.toBinaryString((int)bytes2[i]));
//			 if (i==3)
//				 break;
//		 }
//		 if(true)
//			 System.exit(0);
		 
		 //-- open default rec device --//

		 
//		 byte[] bytes = defaultRecDeviceName.getBytes(Charset.forName("CP1252")); // UTF-16LE
//		 System.out.println(new String (bytes, Charset.forName("UTF-8")));
//		 for(byte b : defaultRecDeviceName.getBytes(Charset.forName("ascii"))){
//			 System.out.println(b + "\t" + (char)(b) + "\t" + Integer.toBinaryString(b));
//		 }
//		 System.out.println("\nMA");
//		 for(byte b : defaultRecDeviceName.getBytes(Charset.forName("Shift-JIS"))){
//			 System.out.println(b + "\t" + (char)(b) + "\t" + Integer.toBinaryString(b));
//		 }
//		 System.out.println("MA");
//		 for(byte b : "マ".getBytes(Charset.forName("UTF-8"))){
//			 System.out.println(b + "\t" + (char)(b) + "\t" + Integer.toBinaryString(b));
//		 }
//		 3 0 D E  
//		 System.out.println(new String(defaultRecDeviceName.getBytes(), Charset.forName("ascii")));
		 
//		 System.out.println(Charset.defaultCharset());
//		 
//		 Set<String> charsets1 = Charset.availableCharsets().keySet();
//		 List<String> charsets2 = new ArrayList<String>(charsets1);
//		 for(String set1 : charsets1){
//			 for(String set2  : charsets2){
//				 System.out.print(set1 + " > " + set2 + "\t");
//				 try{
//					 System.out.println(new String(defaultRecDeviceName.getBytes(Charset.forName(set1)), Charset.forName(set2)));
//				 }catch(UnsupportedOperationException e){
//					 System.out.println("-NOT-SUPPORTED-");
//				 }
//			 }
//		 }
	}
	
	@Override
	synchronized public boolean setRecorderDevice(int ix) throws IllegalStateException{
		if(currentRecDev==null || currentRecDev.equals(AL.getDevice())){
			logger.info("Closing device " + AL.getDevice());
			ALC11.alcCaptureCloseDevice(AL.getDevice());
			logger.info("Closing device " + AL.getDevice());
		}
		if(currentRecDevId!=DEVICE_UNSET && currentRecDev!=null){ //-- If rec device is already set
			if(currentRecDevId==ix){ //-- but request is same as the current
				//-- recDevList==null if OpenAL can't use devicenames because of the device name encoding bug 
				final String devName = (recDevList==null || currentRecDevId== DEVICE_DEFAULT) /* already using default device? */?  "DEFAULT DEVICE" : recDevList[currentRecDevId] ;  
				logger.info("Capture device is already set to '" + devName + "' -- do nothing");
				return true;
			}
		}
		
		if(currentRecDev!=null /* || currentRecDevId >= 0*/ ){ //-- otherwise close previous device
			
			ALC11.alcCaptureCloseDevice(currentRecDev);
		}
		
		if(ix == DEVICE_DEFAULT){
			final String devName = recDevList==null ?  "DEFAULT DEVICE" : recDevList[defaultRecDeviceIx] ;
			logger.info("Openinc caputre device '" + devName + "'");
			currentRecDev = ALC11.alcCaptureOpenDevice(null /* deviceId */, //-- will open the default 
					rformat.hz, rformat.alFormat, sampleBuffSzInSamples /* buff */ );
			currentRecDevId = DEVICE_DEFAULT;
			return true;
		}
		
		if(currentRecDevId > recDevList.length || ix < 0 /* wrong redDevice ID */){
			logger.error("Cant open capture device #" + currentRecDevId +"! Device index must be in the range of [0, " + recDevList.length + "]");
			logger.error("Falling back to DEFAULT device with params '" +rformat+ "'" );
			currentRecDev = ALC11.alcCaptureOpenDevice(null /* deviceId */, //-- will open the default 
					rformat.hz, rformat.alFormat, sampleBuffSzInSamples /* buff */ );
			defaultRecDeviceIx = DEVICE_DEFAULT;
			return true;
		}
		
		currentRecDevId = ix;
		logger.info("Opening CAPTURE device : [" + currentRecDevId + "] '" + recDevList[currentRecDevId] + "' with params '" +rformat+ "'" );
		try{
			currentRecDev = ALC11.alcCaptureOpenDevice(recDevList[currentRecDevId] /* deviceId */, 
					rformat.hz, rformat.alFormat,     sampleBuffSzInSamples /* buff */ );
		}catch (Exception e){
			logger.error("Can't open device ! " + e.toString());
			String defaultRecDeviceNameASCII = new String(recDevList[currentRecDevId].getBytes(Charset.forName("Shift-JIS")), Charset.forName("ASCII"));
			logger.error("Trying SJIS name of the device  " + defaultRecDeviceNameASCII);
			try{ //-- using JSIS
				currentRecDev = ALC11.alcCaptureOpenDevice(defaultRecDeviceNameASCII, rformat.hz, rformat.alFormat, sampleBuffSzInSamples);
			}catch (Exception stillError){
				logger.error("Still can't open device ! " + stillError.toString());
				logger.error("Falling back to defualt device : [" + DEVICE_DEFAULT + "] '" +  recDevList[currentRecDevId] + "' with params '" +rformat+ "'" );
				currentRecDev = ALC11.alcCaptureOpenDevice(null /* deviceId */, //-- will open the default  
						rformat.hz, rformat.alFormat, sampleBuffSzInSamples /* buff */ );
				currentRecDevId = DEVICE_DEFAULT;
			}
		}
		 int err = ALC10.alcGetError(currentRecDev);
		 if(err != 0 )
			 throw new IllegalStateException("Cant open capture device " + recDevList[currentRecDevId] + " with params '" +rformat+ "'" );
		 return true;
	}
	
	@Override
	public List<String> getRecorderDeviceNames(){
		if(_createDefaultAL()){
			if(recDevList==null){
				logger.warn("Device list is not available!");
				return new ArrayList<String>();
			}else{
				List<String> devNames = new ArrayList<String>(recDevList.length);
				for(int i = 0 ; i < recDevList.length ; i++)
					devNames.add(recDevList[i]);
				return devNames;
			}
		}else{
			logger.warn("AL object hasn't been created!");
			return null;
		}
	}	
	
	@Override
	public void startRec(){
		if(isRecording()){ //-- already recording! --//
			return;
		}
		isRecording(true);
		//-- start recording in NEW thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				reInit();
				_capture();
			}
		}).start();
	}
	
	@Override
	synchronized public void stopRec(){
		logger.info("Stop REC request!");
		isRecording(false);
		isRecInterruptReq = true;
	}
//	synchronized private boolean isRecInterruptReq(){
//		return isRecInterruptReq;
//	}
//	
	
	@Override
	public WavClip getWavClip() {
		if(recordedWav==null)
			buildWav();
		return recordedWav;
	}

	static public void disposeAll(){
//		if(currentRecDev!=null)
//			ALC11.alcCaptureCloseDevice(currentRecDev);
		singleton.dispose();
//		ALC11.alcCaptureCloseDevice(device);
//		ALC11.alcCaptureCloseDevice(ALC10.alcGetContextsDevice(ALC10.alcGetCurrentContext()));
//		for(String dev : recDevList){
	}
	
	@Override
	public void dispose(){
		if(recDevList!=null && recDevList.length > 0 && currentRecDevId > -1){
			logger.debug("Closing CAPTURE device  [" + recDevList[currentRecDevId] + "]");
		}else{
			logger.debug("Closing CAPTURE device");
		}
		if(sampleBufferStorage!=null){
			logger.debug("Closing CAPTURE device: clear buffers");
			sampleBufferStorage.clear();
		}
		if(currentRecDev!=null){
			logger.debug("Closing CAPTURE device: close device " + currentRecDev );
			ALC11.alcCaptureCloseDevice(currentRecDev);
		}else{
			logger.debug("No CAPTURE device to close");
		}
	}
	
	/**
	 * Capture is somewhat complicated.
	 * AL native code 
	 * <ul>
	 *   <li> captures sound samples to a ring-buffer
	 *   <li> notifies about available samples every recResolutionInSec (e.g., 1 sec).
	 *   <li> inner buffer should be larger than the recResolutionInSec!
	 *   <li> the samples are copied to an array (more precisely to a List of Arrays)
	 *   <li> this byte array is converted to a WavClip at the end
	 * </ul>
	 * About interrupting
	 * <ul>
	 *   <li>  if 'isRecInterruptReq' flag is true, one more last copy session from inner ring buffer takes place
	 *   <li>  if no available samples in ring buffer for long than 'recResolutionInSec' -> no audio input! 
	 * </ul> 
	 * 
	 * 
	 * @throws IllegalStateException if there is no audio to capture (tested for 1.5 * recResolutionInSec)
	 *  
	 */
	private void _capture() throws IllegalStateException{
		logger.debug("REC thread started " + Thread.currentThread() + " device '" + currentRecDevId + "'  (" + currentRecDev + ")");
		ALC11.alcCaptureStart(currentRecDev);
		int notAvailCnt = 0; //-- how many times samples were not available
		int delay = 20;
		int noAvailMax = (int) Math.ceil(1.5 * recResolutionInSec * 1000.0f/delay);
		
		//-- start to capture to inner ring buffer
		 REC_LOOP:while (true){
			 //-- get number of available samples into 'availableSamples'
			 ALC10.alcGetInteger(currentRecDev, ALC11.ALC_CAPTURE_SAMPLES, availableSamples);
			 avail = availableSamples.get(0);
			 //-- copy to buffer if anything is available
//			 System.out.println("Available  " + avail + " " + notAvailCnt + " / " + noAvailMax);
			 if(avail == 0)
				 notAvailCnt++;
			 while(avail > 0){
//				 System.out.println("Available " + avail);
				 int availInBytes = avail*rformat.recBytePerSample ;
				 int toCopyInSamples = (availInBytes + sampleBuffBytePos) <=  sampleBuffSzInBytes ? avail 
						 :  (sampleBuffSzInBytes - sampleBuffBytePos)/rformat.recBytePerSample;
				 ALC11.alcCaptureSamples(currentRecDev, sampleBuff, toCopyInSamples);
				 sampleBuffBytePos += toCopyInSamples*rformat.recBytePerSample;
				 sampleBuff.position(sampleBuffBytePos);
				 avail -= toCopyInSamples;
//				 System.out.println("#" + sampleBufferStorage.size() + "  [" + sampleBuffBytePos + "/" + sampleBuffSzInBytes + "]   av " + avail);

				 //-- create new buffer if the current is full
				 if(sampleBuffBytePos == sampleBuffSzInBytes){
//					 if(sampleBuffBytePos > sampleBuffSzInBytes){
//						 System.out.println(sampleBuffBytePos +" >"+ sampleBuffSzInBytes);
//						 System.exit(-1);
//					 }
						 
//					 recBuffStorageSizeInKb = sampleBufferStorage.size() * sampleBuffSzInBytes / 1024 ;
//					 double recBuffStorageSizeInSec = ((double) sampleBufferStorage.size() * sampleBuffSzInSamples) / (double)rformat.hz;
//					 System.out.println("Size " + recBuffStorageSizeInKb + " kbyte "+ recBuffStorageSizeInSec+ " sec");
					 if(recBuffStorageSizeInKb >= AUDIO_MAX_SIZE_IN_KBYTE){
						 break REC_LOOP;
					 }
					 sampleBuff = org.lwjgl.BufferUtils.createByteBuffer(sampleBuffSzInBytes);
					 sampleBufferStorage.add(sampleBuff);
					 sampleBuffBytePos = 0;
//					 System.out.println("#" + sampleBufferStorage.size() + "  [" + sampleBuffBytePos + "/" + sampleBuffSzInBytes + "]   av " + avail);
				 }
				 if(isRecInterruptReq){
					 logger.debug(" stopped requested but still avail " + avail);
				 }
				 if(avail==0 && isRecInterruptReq){
					 break REC_LOOP;
				 }
				 notAvailCnt = 0;
			  } //-- while REC loop
			 
			 
			 if(notAvailCnt >= noAvailMax){
				logger.error("There is no Audio Input! Stopping recording!");
				ALC11.alcCaptureStop(currentRecDev);
				isRecording(false);
				return;
//				throw new IllegalStateException("There is no Audio Input!");
		 	 }
			 try{ //-- To delay buffer checks.. 
				  //-- Avoid too frequent loops!
				 Thread.sleep(delay /*+rand.nextInt(200)*/ );
			 }catch(InterruptedException e){};
			 
		 }//-- while REC_LOOP
		ALC11.alcCaptureStop(currentRecDev);
		isRecording(false);
	}
	
	volatile boolean isRecording = false;
	
	
	private void buildWav(){
		//-- stop recording : in case it's still going on.. --//
		stopRec();
//		isRecInterruptReq = true;
		while(isRecording()){
			//-- wait till recording is finished --//
			try{
				logger.warn("Can't build wav while recording : retrying.. ");
				Thread.sleep(500);
			}catch(InterruptedException e){
				break;
			}
		}
		 
		int audioLenInBytes = sampleBuffSzInBytes*(sampleBufferStorage.size()-1) + sampleBuffBytePos;
		byte[] buff = new byte[sampleBuffSzInBytes];
		ByteArrayOutputStream baos = new ByteArrayOutputStream(audioLenInBytes);
//		int off = 0;    //-- check this: This : why was this needed?
		int bi = 0 ;
		for(; bi < sampleBufferStorage.size()-1; bi++){
//			System.out.println("#"+bi + ". array  " + sampleBufferStorage.get(bi).position());
			sampleBufferStorage.get(bi).position(0);
			sampleBufferStorage.get(bi).get(buff, 0, sampleBuffSzInBytes);
			baos.write(buff, 0, sampleBuffSzInBytes);
//			off += sampleBuffSzInBytes;
		}
		sampleBufferStorage.get(bi).position(0);
		sampleBufferStorage.get(bi).get(buff, 0, sampleBuffBytePos);
		baos.write(buff, 0, sampleBuffBytePos);
//		off += sampleBuffBytePos;
		try{
			baos.close();
		}catch(IOException e){};

		//-- clear up
		sampleBufferStorage.clear();
		
//		logger.info(off/1024.0f +" kbytes written.");
		logger.info(String.format("%.2f kbytes captured", (audioLenInBytes/1024.0f))); 
		recordedWav = new WavClip(baos, new AudioFormat(rformat.hz, rformat.recBytePerSample*8, 1, false/*signed*/, false/*bigEndian*/));
	}
	

	
	
	public static void main(String[] args) throws Exception {
//		BasicConfigurator.configure();

		final OpenAlRecorder rec = OpenAlRecorder.getInstance();
		rec.init(RecFormat.MONO16_16k);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("START RECORDING!");
				rec.startRec();
			}
		}).start();
		
		Thread.sleep(600);
		System.out.println("STOP RECORDING");
		rec.stopRec();
		WavClip wav = rec.getWavClip();
		System.out.println(wav);
		Thread.sleep(600);
		
		OpenAlRecorder.disposeAll();
	}
		/*
//		OpenAlPlayer pl = new OpenAlPlayer();
////		WavClip wav_ =  new WavClip("data/snd/sample.wav");
////		pl.initWav(wav_);
////		pl.play();
//		
		rec.setRecorderDevice(2);
//		
		new Thread(new Runnable() {
			@Override
			public void run() {
//				System.out.println("START RECORDING!");
				rec.startRec();
				
			}
		}).start();
//		System.out.println("Recording thread is back!");
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
//					System.out.println(rec.isRecording());
					try{
						Thread.sleep(250);
					}catch(InterruptedException e){
						e.printStackTrace();
					};
				}
			}
		}).start();
		
		try{
			Thread.sleep(3000);
		}catch(InterruptedException e){
			e.printStackTrace();
		};
		rec.stopRec();
		try{
			Thread.sleep(3000);
		}catch(InterruptedException e){
			e.printStackTrace();
		};
//		
//		WavClip wav = rec.getWavClip();
//		
//		System.out.println("START REPLAY!");
//		pl.initWav(wav);
//		pl.play();
//		try{
//			Thread.sleep(6500);
//		}catch(InterruptedException e){};
		OpenAlRecorder.disposeAll();
		OpenAlPlayer.disposeAll();
	}
	 */

}
