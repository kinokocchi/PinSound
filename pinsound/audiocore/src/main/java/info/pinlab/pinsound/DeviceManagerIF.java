package info.pinlab.pinsound;

import java.util.List;

public interface DeviceManagerIF {
	public List<String> getPlayerDeviceNames();
	public boolean setPlayerDevice(int ix);
	public String getCurrentPlayerDeviceName();
	
//	public PlayerControlIF getPlayer(WavClip wav);
}
