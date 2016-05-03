/**
 * Created by leonard on 16/4/29.
 */
package main;

import gopush.*;
import org.junit.Assert;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson.JSONObject;
public class main {
    public static void main(String[] args) {
        post();
        ThreadLocal<GoPushCli> local = new ThreadLocal<GoPushCli>();
        local.set(new GoPushCli("localhost", 8090, "leonard", 30, new Listener() {
            @Override
            public void onOpen() {
                logutil.debug("dang dang dang dang~~~~~~~~~~~~~~~~onOpen");
            }

            @Override
            public void onOnlineMessage(PushMessage message) {
                logutil.debug("online message: " + message.getMsg());
                logutil.debug("online message id: " + message.getMid());
            }

            @Override
            public void onOfflineMessage(List<PushMessage> messages) {
                if (messages != null)
                    for (PushMessage message : messages) {
                        logutil.debug("offline message: " + message.getMsg());
                        logutil.debug("offline message id: " + message.getMid());
                    }
            }

            @Override
            public void onError(Throwable e, String message) {
                logutil.error(e+":"+message);
            }

            @Override
            public void onClose() {logutil.debug("onClose~~~~~~~~~~");}
        }));


        //====================begin1===============
        logutil.error("====================begin===============");
        GoPushCli cli = local.get();
        cli.start(false, 0, 0);
        try {
            TimeUnit.SECONDS.sleep(100 * 1000);
        } catch (InterruptedException e) {
            logutil.error(e);
        }

        Assert.assertTrue("获取节点失败", cli.isGetNode());
        Assert.assertTrue("握手失败", cli.isHandshake());
        cli.destory();
        logutil.error("=======================end===================");
    }
        public static void post(){
            JSONObject obj = new JSONObject();
            obj.put("name","leonard");
            obj.put("age", 23);
            try {
                String resp=HTTPUtils.post("http://localhost:8091/1/admin/push/private?key=leonard&expire=600", obj.toString());
                logutil.debug(resp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
