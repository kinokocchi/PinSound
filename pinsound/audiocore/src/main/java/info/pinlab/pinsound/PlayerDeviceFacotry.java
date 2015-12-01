package info.pinlab.pinsound;

public interface PlayerDeviceFacotry {
	public PlayerDeviceController getPlayer();
	public PlayerDeviceController getPlayer(WavClip wav);
	public void disposeAll();
	public void stopAll();
}
