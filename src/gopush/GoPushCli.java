package gopush;

import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;


public class GoPushCli {
	/**
	 * 初始化GopushCli
	 * 
	 * @param host
	 *            web模块的host
	 * @param port
	 *            web模块的端口
	 * @param key
	 *            订阅的key
	 * @param expire
	 *            设置过期和心跳时间（单位：秒）
	 * @param mid
	 *            设置上次接受私信推送以来最大的消息ID
	 * @param pmid
	 *            设置上次接受公信推送以来最大的消息ID
	 * @param listener
	 *            设置监听器
	 */
	public GoPushCli(String host, Integer port, String key, Integer heartbeat, Listener listener) {
		this.interfaces = new HTTPInterfaces(host, port);
		this.key = key;
		this.heartbeat = heartbeat;
		this.listener = listener;
	}

	/**
	 * 开始订阅
	 * 
	 * @param isSync
	 *            true: 同步订阅，会阻塞在start函数。 false: 异步订阅，订阅成功后会返回。
	 */
	public void start(boolean isSync, long mid, long pmid) {
		try {
			this.node = interfaces.getNode(key, Proto.TCP);
			this.node.refreshMid(mid);
			this.node.refreshPmid(pmid);
			init(); // 初始化socket
			listener.onOpen();// 协议已经握手，打开
			listener.onOfflineMessage(interfaces.getOfflineMessages(key, node));// 加载离线消息

			heartbeatTask = new Thread(new HeartbeatTask()); // 准备定时心跳任务
			heartbeatTask.start();
		} catch (Exception e) {
			listener.onError(e, e.getMessage());
			destory();
			return;
		}

		// 如果是同步协议，block
		if (isSync) {
			try {
				crawl();
			} catch (Exception e) {
				listener.onError(e, e.getMessage());
			} finally {
				destory();
			}
		} else {
			// 异步，nonblock
			new Thread() {
				public void run() {
					try {
						crawl();
					} catch (Exception e) {
						listener.onError(e, e.getMessage());
					} finally {
						destory();
					}
				}
			}.start();
		}
	}

	private void init() throws Exception {
		try {
			logutil.debug("init..............." + node.getHost() + ":" + node.getPort());
			this.socket = new Socket();
			this.socket.setKeepAlive(true);
			// 两倍超时时间
			this.socket.setSoTimeout((heartbeat + 15) * 1000);
			this.socket.connect(new InetSocketAddress(node.getHost(), node.getPort()));
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			// 发送请求协议头
			sendHeader();
		} catch (Exception e) {
			throw new Exception("初始化套接字错误", e);
		}
	}

	public void destory() {
		if (isDesotry) {
			return;
		}
		isDesotry = true;
		listener.onClose();
		listener = new Listener() {
		};
		// 关闭连接 释放线程
		if (heartbeatTask != null && !heartbeatTask.isInterrupted()) {
			heartbeatTask.interrupt();
		}
		if (socket != null && !socket.isClosed() && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	private void sendHeader() throws Exception {
		String heartbeatStr = heartbeat.toString();
		String protocol = "*3\r\n$3\r\nsub\r\n$" + key.length() + "\r\n" + key + "\r\n$" + heartbeatStr.length() + "\r\n" + heartbeatStr + "\r\n";

		// 发送请求协议
		send(protocol);
		String response = receive();
		if (response.startsWith("+")) {
			// 初始心跳
			isHandshake = true;
		} else if (response.startsWith("-")) {
			// 协议错误
			throw new Exception("comet节点握手协议错误: " + response);
		} else {
			throw new IllegalArgumentException("无法识别comet返回协议: " + response);
		}
	}

	private void send(String message) {
		assert socket != null;

		logutil.debug("send........."+message);
		writer.print(message);// 这里原本实现是没有加换行符的
		writer.flush();
	}

	private String receive() throws IOException {
		assert socket != null;

		return reader.readLine();
	}

	private void crawl() throws Exception {
		String line = null;
		try {
			logutil.debug("crawl.......");
			while ((line = receive()) != null) {
				logutil.debug("receive......."+line);
				if (line.startsWith("+")) {
					// 忽略心跳
				} else if (line.startsWith("$")) {
					line = receive();
					PushMessage message = (PushMessage) JSONObject.parseObject(line, PushMessage.class);
					boolean isRefresh = false;
					if (message.isPub()) {
						isRefresh = node.refreshPmid(message.getMid());
					} else if (!message.isPub()) {
						isRefresh = node.refreshMid(message.getMid());

					}
					if (isRefresh) {
						listener.onOnlineMessage(message);
					}
				} else if (line.startsWith("-")) {
					throw new Exception("comet节点订阅协议错误: " + line);
				}
			}
		} catch (IOException e) {
			throw new Exception("获取comet节点订阅数据网络数据失败", e);
		}
	}

	public boolean isGetNode() {
		return node != null;
	}

	public boolean isHandshake() {
		return isHandshake;
	}

	class HeartbeatTask implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				// 发送心跳
				send("h");
				try {
					logutil.debug("heartbeat:......."+heartbeat);
					TimeUnit.SECONDS.sleep(heartbeat);
				} catch (InterruptedException e) {
					System.err.println("Timer is stop:"+e);
					break;
				}
			}
		}
	}

	// 对象属性
	private Node node;// 节点
	private String key; // subscriber key
	private Integer heartbeat; // heartbeat second
	private HTTPInterfaces interfaces;
	private Listener listener;
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	private Thread heartbeatTask;

	private boolean isHandshake = false;
	private boolean isDesotry = false;

}
