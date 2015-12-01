package info.pinlab.snd.oal;

import org.lwjgl.Sys;

import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.PlayerDeviceFacotry;
import info.pinlab.pinsound.WavClip;


public class OpenAlPlayerFactory implements PlayerDeviceFacotry {

	public OpenAlPlayerFactory(){ 	
		Sys.initialize();
	}
	
	@Override
	public PlayerDeviceController getPlayer(){
		return new OpenAlPlayer();
	}

	@Override
	public PlayerDeviceController getPlayer(WavClip wav) {
		OpenAlPlayer player = new OpenAlPlayer();
		player.initWav(wav);
		return player;
	}

	
	
	public void disposeAll(){
		OpenAlPlayer.disposeAll();
	}

	@Override
	public void stopAll() {
		for(OpenAlPlayer player: OpenAlPlayer.players){
			player.stop();
		}
	}
}
