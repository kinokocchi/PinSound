package info.pinlab.snd.oal;

import org.junit.Test;

public class OpenAlLoadTest {

	@Test
	public void test() throws Exception{
		new OpenAlPlayer();
		
		Thread.sleep(200);
		OpenAlPlayer.disposeAll();
	}
}
