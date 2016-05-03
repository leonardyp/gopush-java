package gopush;

import java.util.List;

public abstract class Listener {
	public void onClose() {
	}
	public void onOfflineMessage(List<PushMessage> messages) {
	}

	public void onOnlineMessage(PushMessage message) {
	}
	public void onOpen() {
	}
	public void onError(Throwable e, String message) {
	}
}
