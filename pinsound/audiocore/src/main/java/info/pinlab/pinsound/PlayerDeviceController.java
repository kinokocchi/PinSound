package info.pinlab.pinsound;

public interface PlayerDeviceController extends PlayerDeviceMonitor {

//	public void initWav(WavClip wav) throws IllegalStateException;

	/**
	 *  Starts play in a different thread
	 */
	public void play();
	
	/**
	 * @return current position.
	 */
	public long pause();
	public void stop();
	
	/**
	 * 
	 * @return position in frames
	 */
	public void setCursorPosInMs(long startPosInFrames);

	
//	public void addPlayerReqListener(PlayerReqListener l);
	
	public void initWav(WavClip wav) throws IllegalStateException;
	
	public void dispose();
}