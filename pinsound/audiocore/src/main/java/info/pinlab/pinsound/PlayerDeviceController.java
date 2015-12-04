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
	 * @param startPosInFrames  start frame (inclusive)
	 */
	public void setCursorPosInMs(long startPosInFrames);

	
//	public void addPlayerReqListener(PlayerReqListener l);
	
	public void initWav(WavClip wav) throws IllegalStateException;
	
	public void dispose();
}