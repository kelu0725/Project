package cc.cmu.edu.encoder;

import static org.junit.Assert.*;

import org.junit.Test;

public class EncoderTest {
	@Test
	public void testPayloadArray() {
		Encoder encoder = new Encoder();
		System.out.println(encoder.encode("CC Team"));
		assertTrue(encoder.encode("CC Team").equals("0xfe33fc140xd06ea2bb0x7595dbac0xaec121070xfaafe00c0xb32450xce10b7980x490419c0x2340001c0x47f844300x5315bac90x25d18c2e0xa60505140x1fd8a2"));
		assertTrue(encoder.encode("CC Team is awesome!").equals("0xfe453fc10x106e950x4bb75aa50xdba102ec0x100907fa0xaafe00800x2338280x8aaa0600xea1228b40x4a0040x42cd56410xcea034790x81de020xff000a440x7f802b300x42b14ba50x5f85d02d0x4ee840450x42820fe0x17e00"));
	}

}
