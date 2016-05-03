package test;

import gopush.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GoPushCliTest {
	@Before
	public void init() {
		local.set(new GoPushCli("localhost", 8090, "leonard", 30, new Listener() {
			@Override
			public void onOpen() {
				System.out.println("dang dang dang dang~onOpen");
			}
			@Override
			public void onOnlineMessage(PushMessage message) {
				System.out.println("online message: " + message.getMsg());
				System.out.println("online message id: " + message.getMid());
			}

			@Override
			public void onOfflineMessage(List<PushMessage> messages) {
				if (messages != null)
					for (PushMessage message : messages) {
						System.out.println("offline message: " + message.getMsg());
						System.out.println("offline message id: " + message.getMid());
					}
			}

			@Override
			public void onError(Throwable e, String message) {
				Assert.fail(message);
			}

			@Override
			public void onClose() {
				System.out.println("pu pu pu pu~");
			}
		}));
	}

	@Test
	public void testNoSync() {
		GoPushCli cli = local.get();
		cli.start(false, 0, 0);

		Assert.assertTrue("获取节点失败", cli.isGetNode());
		Assert.assertTrue("握手失败", cli.isHandshake());
		cli.destory();
	}

	@Test
	public void testSync() {
		final GoPushCli cli = local.get();
		new Thread() {
			public void run() {
				cli.start(true, 0, 0);
			}
		}.start();
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
		}
		Assert.assertTrue("获取节点失败", cli.isGetNode());
		Assert.assertTrue("握手失败", cli.isHandshake());
		cli.destory();
	}

	private ThreadLocal<GoPushCli> local = new ThreadLocal<GoPushCli>();
}
