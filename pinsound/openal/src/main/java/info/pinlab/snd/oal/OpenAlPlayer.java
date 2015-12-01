package info.pinlab.snd.oal;


import info.pinlab.pinsound.DeviceManagerIF;
import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.util.WaveData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Plays a single audio file.
 * <ul>
 *  <li> to play more than one audio @see {@link OpenAlMultiPlayer}
 *  <li> to record audio @see {@link OpenAlRecorder}
 * </ul>
 * 
 * @author Gabor Pinter
 *
 */
public class OpenAlPlayer implements PlayerDeviceController, DeviceManagerIF {
	private static final Logger LOG = LoggerFactory.getLogger(OpenAlPlayer.class);
	private WavClip currentWav = null;

	float recResolutionInSec = Sys.getTimerResolution()/1000.0f;
	static String [] playDeviceList = null;
	private final int playerDeviceId = 0;

	//	  boolean  contextSynchronized = false;
	//	  int contextRefresh = 15;
	//	  int contextFrequency = 44100;
	//	  String deviceArguments = "PulseAudio Default" ;


	protected static Set<OpenAlPlayer> players = new HashSet<OpenAlPlayer>();

	/** Buffers hold sound data. */
	IntBuffer samples = BufferUtils.createIntBuffer(1);
	/** Sources are points emitting sound. */
	IntBuffer sndEmittingSrc = BufferUtils.createIntBuffer(1);

	/** Position of the source sound. */
	final private FloatBuffer sourcePos = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });
	/** Velocity of the source sound. */
	final private FloatBuffer sourceVel = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });
	/** Position of the listener. */
	final private FloatBuffer listenerPos = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });
	/** Velocity of the listener. */
	final private FloatBuffer listenerVel = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });
	/** Orientation of the listener. (first 3 elements are "at", second 3 are "up") */
	final private FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });

	static{
		Sys.initialize();
	}

	public OpenAlPlayer(){
		sourcePos.position(0);
		sourceVel.position(0);
		listenerPos.position(0);
		listenerVel.position(0);
		listenerOri.position(0);
		OpenAlPlayer.players.add(this);
		_createDefaultAL();
		getPlayerDeviceNames();
	}
	/**
	 * boolean LoadALData()
	 *
	 *  This function will load our sample data from the disk using the Alut
	 *  utility and send the data into OpenAL as a buffer. A source is then
	 *  also created to play that buffer.
	 */
	int loadALData(WavClip wav) {
		//-- clear err msg
		AL10.alGetError();
		// Load wav data into a buffer.
		AL10.alGenBuffers(samples);

		if(AL10.alGetError() != AL10.AL_NO_ERROR){
			LOG.error("AL10.alGenBuffers failed! Samples " + samples);
			return AL10.AL_FALSE;
		}

		// Loads the wave file from this class's package in your classpath
		WaveData waveFile = WaveData.create(wav.getAudioInputStream());

		AL10.alBufferData(samples.get(0), waveFile.format, waveFile.data, waveFile.samplerate);

		waveFile.dispose();

		// Bind the buffer with the source.
		AL10.alGenSources(sndEmittingSrc);

		if (AL10.alGetError() != AL10.AL_NO_ERROR)
			return AL10.AL_FALSE;

		sourcePos.position(0);
		sourceVel.position(0);
		AL10.alSourcei(sndEmittingSrc.get(0), AL10.AL_BUFFER,   samples.get(0) );
		AL10.alSourcef(sndEmittingSrc.get(0), AL10.AL_PITCH,    1.0f          );
		AL10.alSourcef(sndEmittingSrc.get(0), AL10.AL_GAIN,     1.0f          );
		AL10.alSource (sndEmittingSrc.get(0), AL10.AL_POSITION, sourcePos     );
		AL10.alSource (sndEmittingSrc.get(0), AL10.AL_VELOCITY, sourceVel     );

		//	    System.out.println("Gain " + AL10.alGetSourcef(sndEmittingSrc.get(0), AL10.AL_GAIN));
		// Do another error check and return.
		if (AL10.alGetError() == AL10.AL_NO_ERROR)
			return AL10.AL_TRUE;

		return AL10.AL_FALSE;
	}

	@Override
	synchronized public void dispose(){
		stop();
		killALData();
		currentWav = null;
	}
	
	/**
	 * void killALData()
	 *
	 *  We have allocated memory for our buffers and sources which needs
	 *  to be returned to the system. This function frees that memory.
	 */
	void killALData(){
		//-- clear error bit
		int AL10_err = AL10.alGetError() ;
		
		//-- delete buffers
		if(AL10.alIsSource(sndEmittingSrc.get(0))){
			AL10.alDeleteSources(sndEmittingSrc);
			AL10.alDeleteBuffers(samples);
		}
		AL10_err = AL10.alGetError() ;
		if (AL10_err != AL10.AL_NO_ERROR){
			LOG.error("Error wihle killing data! Error code " + AL10_err );
		}
	}

	@Override
	public List<String> getPlayerDeviceNames(){
		if(_createDefaultAL()){
			String devices = ALC10.alcGetString(null, ALC10.ALC_DEVICE_SPECIFIER);
			playDeviceList = devices.split("\0");
			List<String> devNames = new ArrayList<String>(playDeviceList.length);
			for(int i = 0; i < playDeviceList.length ; i++){
				LOG.info("Available audio PLAY device : [" + (i) + "] '"  + playDeviceList[i] + "'");
				devNames.add(playDeviceList[i]);
			}
			return devNames;
		}else{
			return null;
		}
	}




	//-- You need to re-init the data after setting the player device!	
	@Override
	public boolean setPlayerDevice(int ix) {
		if(ix >= playDeviceList.length){
			LOG.error("There are only " + playDeviceList.length + " devices. You want #" + (ix+1));
			return false;
		}
		if(!AL.isCreated()){
			_createDefaultAL();
		}
		String deviceName = playDeviceList[ix] ;
		//		ALCcontext oldContext = ALC10.alcGetCurrentContext();
		//		ALC10.alcDestroyContext(oldContext);

		ALCdevice device = ALC10.alcOpenDevice(deviceName);

		if(device !=null && device.isValid()){
			ALCcontext context = ALC10.alcCreateContext(device, null);
			ALC10.alcMakeContextCurrent(context);
			LOG.info("Creating context '" + deviceName + "'" );
			return true;
		}else{
			LOG.info("Couldn't create context '" + deviceName + "'" );
			return false;
		}
		//		alcGetString(AL.ALC_DEVICE_SPECIFIER);
	}

	@Override
	public String getCurrentPlayerDeviceName(){
		ALCcontext context = ALC10.alcGetCurrentContext();
		ALCdevice device = ALC10.alcGetContextsDevice(context);
		return ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER);
	}



	private boolean _createDefaultAL(){
		try{
			if(!AL.isCreated()){
				AL.create();
				ALCcontext context =  AL.getContext();
				System.out.println(context);
				//				AL.create(null, contextFrequency, contextRefresh, contextSynchronized);
				LOG.info("OpenAL native resource created");
			}
		}catch (LWJGLException le) {
			for(StackTraceElement e : le.getStackTrace()){
				LOG.error(e.getClassName() + "#" + e.getMethodName());
			}
			le.printStackTrace();
			return false;
		}
		//-- clear error bit
		AL10.alGetError();
		isDestroyed = false;
		return true;
	}



	@Override
	synchronized public void initWav(WavClip wav) throws IllegalStateException{
		LOG.debug("Init wav : " + wav);
		if(currentWav!=null){
			LOG.debug("Flushing previous wav sound " + currentWav);
			this.killALData();
		}

		currentWav = wav ;
		if(!_createDefaultAL()){
			currentWav = null;
			throw new IllegalStateException("Couldn't create OpenAL player '" + playDeviceList[playerDeviceId]+"'");
		}


		// Load the wav data.
		if(loadALData(currentWav) == AL10.AL_FALSE) {
			LOG.error("Error loading data: " + currentWav);
			currentWav = null;
			throw new IllegalStateException("Couldn't load audio " + wav.toLongString());
		}
		
		//-- init sound
		sourcePos.position(0);
		sourceVel.position(0);
		listenerPos.position(0);
		listenerVel.position(0);
		listenerOri.position(0);

		/**
		 *  We already defined certain values for the Listener, but we need
		 *  to tell OpenAL to use that data. This function does just that.
		 */
		AL10.alListener(AL10.AL_POSITION,    listenerPos);
		AL10.alListener(AL10.AL_VELOCITY,    listenerVel);
		AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
	}

	
	private static boolean isDestroyed = false;
	
	public static void disposeAll(){
		if(!AL.isCreated())
			return;
		if(isDestroyed)
			return;
		LOG.info("Closing PLAYER device " + players.size() +  " players/sounds.");
		for(OpenAlPlayer p : players){
			synchronized (p) {
				p.dispose();
			}
		}
		try { //-- give some time to native resources to chill out
			Thread.sleep(100);
		} catch (InterruptedException ignore) {}
		
		LOG.info("Closing OpenAL static resources");
		
		ALCcontext context = ALC10.alcGetCurrentContext();
		ALCdevice device = ALC10.alcGetContextsDevice(context);
		
		ALC10.alcMakeContextCurrent(null);
		ALC10.alcDestroyContext(context);
		ALC10.alcCloseDevice(device);
		isDestroyed = true;
//		AL.destroy();
	}

	@Override
	synchronized public void play(){
		AL10.alSourcePlay(sndEmittingSrc.get(0));
	}

	@Override
	synchronized public long pause(){
		AL10.alSourcePause(sndEmittingSrc.get(0));
		return AL10.alGetSourcei(sndEmittingSrc.get(0), AL11.AL_SAMPLE_OFFSET);
	}
	@Override
	synchronized public void stop() {
		AL10.alSourceStop(sndEmittingSrc.get(0));
	}

	public long getOffset(){
		return AL10.alGetSourcei(sndEmittingSrc.get(0), AL11.AL_SAMPLE_OFFSET);
	}

	@Override
	synchronized public long getPlayPosInMs(){
		if( AL10.AL_STOPPED == AL10.alGetSourcei(sndEmittingSrc.get(0), AL10.AL_SOURCE_STATE)
				&& 
				AL10.AL_PAUSED != AL10.alGetSourcei(sndEmittingSrc.get(0), AL10.AL_SOURCE_STATE)
				){ //-- if STOPPED and NOT FINISHED -> ended!
			return currentWav.getDurInMs(); 
		}
		float f = 1000*AL10.alGetSourcef(sndEmittingSrc.get(0), AL11.AL_SEC_OFFSET);
		return (long)f;
	}

	@Override
	public void setCursorPosInMs(long pos) {
		float p = pos/1000.0f;
		AL10.alSourcef(sndEmittingSrc.get(0), AL11.AL_SEC_OFFSET, p);
	}

	@Override
	public boolean isStopped(){
		if(AL10.AL_STOPPED == AL10.alGetSourcei(sndEmittingSrc.get(0), AL10.AL_SOURCE_STATE))
			return true;
		return false;
	}
	@Override
	public boolean isPaused(){
		if(AL10.AL_PAUSED == AL10.alGetSourcei(sndEmittingSrc.get(0), AL10.AL_SOURCE_STATE))
			return true;
		return false;
	}
	@Override
	public boolean isPlaying(){
		if(AL10.AL_PLAYING == AL10.alGetSourcei(sndEmittingSrc.get(0), AL10.AL_SOURCE_STATE))
			return true;
		return false;
	}	

	public float setGain(float gain){
		if(gain > 1.0f)
			gain=1.0f;
		if(gain<0){
			LOG.error("Can't set volume sub-zero! It's between [0, 1.0f]!");
			AL10.alGetSourcef(sndEmittingSrc.get(0), AL10.AL_GAIN);
		}
		AL10.alSourcef(sndEmittingSrc.get(0), AL10.AL_GAIN, gain);
		return gain;
	}

//	public static void main(String[] args) throws Exception {
//		BasicConfigurator.configure();
//		OpenAlPlayer pl = new OpenAlPlayer();
//		//		InputStream is  = pl.getClass().getResourceAsStream("sample.wav");
//		final WavClip wav =  new WavClip("sample.wav");
//		//		pl.initWav(wav);
//
//		//		final WavClip wav2 = new WavClip("data/snd/sample2.wav");
//		//		pl.initWav(wav);
//		pl.initWav(wav);
//
//		//		pl.setPlayerDevice(0);
//		System.out.println(pl.getCurrentPlayerDeviceName());
//		pl.play();
//		//		pl.initRec();
//		//			System.out.println(pl.getPlayPosInSamples());
//		try{
//			Thread.sleep(2200);
//		}catch (InterruptedException e){};
//		OpenAlPlayer.disposeAll();
//		//		pl.dispose();
//
//		//		pl.initWav(wav2);
//		//		pl.play();
//		//
//		//		while(pl.isPlaying()){
//		////			System.out.println(pl.getPlayPosInSamples());
//		//			try{
//		//				Thread.sleep(200);
//		//			}catch (InterruptedException e){};
//		//		}
//		//
//		//		
//
//		//		float t2 = 0.7894375f;
//		//		pl.setOffset(14974);
//
//		//		long pos = pl.pause();
//		////		float t = AL10.alGetSourcef(pl.sndSrc.get(0), AL11.AL_SEC_OFFSET);
//		//		System.out.println(pos+ " " + pos);
//		//		try{
//		//			Thread.sleep(500);
//		//		}catch (InterruptedException e){};
//		//		pl.play();
//
//		//		try{
//		//			Thread.sleep(3000);
//		//		}catch (InterruptedException e){};
//	}

}