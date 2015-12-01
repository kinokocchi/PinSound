package info.pinlab.snd.app;

import info.pinlab.pinsound.app.AudioPlayerListener;
import info.pinlab.pinsound.app.AudioPlayerView;

public class CliAudioPlayerView implements AudioPlayerView {

	@Override
	public void setPlayingState() {
		System.out.println("..is playing");		
	}

	@Override
	public void setReadyToPlayState() {
		System.out.println("..is ready for playing");		
	}

	@Override
	public void setPlayMaxLenInMs(long ms) {
		System.out.println(".. set max len " + ms +"ms");		
	}

	@Override
	public void setPlayPosInMs(long ms) {
		System.out.println(ms +"ms");
	}

	@Override
	public void setPlayEnabled(boolean b) {
		System.out.println(".. player enabled: " + b );		
	}

	@Override
	public void setPlayActionListener(AudioPlayerListener l) {	}

}
