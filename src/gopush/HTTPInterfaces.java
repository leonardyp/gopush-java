package gopush;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class HTTPInterfaces {
	public Node getNode(String key, Proto proto) throws GopushServerException {
		if (key == null || key.trim().length() == 0 || proto == null) {
			throw new IllegalArgumentException("key 和 proto不能为空");
		}
		String response = null;
		try {
			String url=String.format(GET_NODE, key, proto.getProto());
			logutil.debug("url......."+url);
			response = HTTPUtils.get(url);
			if (response.trim().length() == 0) {
				throw new GopushServerException("服务端响应内容为空");
			}

			JSONObject jo = JSONObject.parseObject(response);
			int ret = jo.getIntValue("ret");
			switch (RetCode.getRetCode(ret)) {
			case SUCCESS:
				JSONObject data = jo.getJSONObject("data");
				if (data == null) {
					throw new GopushServerException("服务端报文有误 [" + response + "]");
				}
				String server = jo.getJSONObject("data").getString("server");
				logutil.debug("getNode....." + server);
				if (server == null || server.trim().length() == 0) {
					throw new GopushServerException("服务端报文有误 [" + response + "]");
				}
				String[] args = server.split(":");

				return new Node(key, args[0], Integer.parseInt(args[1]));
			case NOT_FOUND_NODE:
				logutil.debug("getNode...........1");
				throw new GopushServerException("指定节点已经失效");
			default:
				throw new GopushServerException("错误码: " + ret + ", " + jo.getString("message"));
			}
		} catch (IOException e) {
			throw new GopushServerException("连接异常", e);
		}
	}

	/**
	 * 获取离线消息
	 * 
	 * @param key
	 *            客户端key
	 * @param mid
	 *            私有消息ID
	 * @param pmid
	 *            公开消息ID
	 * @return
	 */
	public List<PushMessage> getOfflineMessages(String key, Node node) throws GopushServerException {
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException("key不可为空");
		} else if (node == null) {
			throw new IllegalArgumentException("node不可为空");
		}
		try {
			String url=String.format(GET_OFFLINE, key, node.getMid(), node.getPmid());
			logutil.debug("getOfflineMessages......." + url);
			String response = HTTPUtils.get(url);
			if (response.trim().length() == 0) {
				throw new GopushServerException("服务端响应内容为空");
			}
			JSONObject jo = JSONObject.parseObject(response);
			int ret = jo.getIntValue("ret");
			RetCode code=RetCode.getRetCode(ret);
			switch (code) {
			case SUCCESS:
				logutil.debug("success...."+jo);
				JSONObject data = jo.getJSONObject("data");
				if (data == null) {
					throw new GopushServerException("服务端报文有误 [" + response + "]");
				} else if (data.isEmpty()) {
					return new LinkedList<PushMessage>();
				}
				List<PushMessage> messages = new LinkedList<PushMessage>();
				JSONArray msgs = data.getJSONArray("msgs");
				if (!msgs.isEmpty()) {
					for (Iterator<?> it = msgs.iterator(); it.hasNext();) {
						PushMessage message = (PushMessage) JSONObject.parseObject(it.next().toString(), PushMessage.class);
						node.refreshMid(message.getMid());
						messages.add(message);
					}
				}
				msgs = data.getJSONArray("pmsgs");
				if (!msgs.isEmpty()) {
					for (Iterator<?> it = msgs.iterator(); it.hasNext();) {
						PushMessage message = (PushMessage) JSONObject.parseObject(it.next().toString(), PushMessage.class);
						node.refreshPmid(message.getMid());
						logutil.debug("messages......."+message);
						messages.add(message);
					}
				}
				return messages;
			case NOT_FOUND_NODE:
				logutil.debug("getOfflineMessages...........");
				throw new GopushServerException("指定节点已经失效");
			default:
				throw new GopushServerException("错误码: " + ret + ", " + jo.getString("message"));
			}
		} catch (IOException e) {
			throw new GopushServerException("连接异常", e);
		}
	}

	public HTTPInterfaces(String host, int port) {
		this(host, port, false);
	}

	public HTTPInterfaces(String host, int port, boolean isSSL) { // TODO 暂时不支持SSL
		String base = "http://" + host + ":" + port;
		GET_NODE = base + "/server/get?key=%s&proto=%d";
		GET_OFFLINE = base + "/msg/get?key=%s&mid=%d&pmid=%d";
	}

	public static void main(String[] args) throws Exception {
		HTTPInterfaces interfaces = new HTTPInterfaces("localhost", 8090);
		Node node = interfaces.getNode("leonard", Proto.TCP);
		interfaces.getOfflineMessages("leonard", node);
	}

	private final String GET_NODE;
	private final String GET_OFFLINE;
}
