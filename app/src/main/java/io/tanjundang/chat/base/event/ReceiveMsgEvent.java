package io.tanjundang.chat.base.event;

import io.tanjundang.chat.base.entity.SocketMsgResp;

/**
 * @Author: TanJunDang
 * @Date: 2017/11/6
 * @Description:
 */

public class ReceiveMsgEvent {

    public ReceiveMsgEvent(SocketMsgResp.SocketMsgInfo info) {
        this.info = info;
    }

    SocketMsgResp.SocketMsgInfo info;
    String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ReceiveMsgEvent(String data) {
        this.data = data;
    }

    public SocketMsgResp.SocketMsgInfo getInfo() {
        return info;
    }

    public void setInfo(SocketMsgResp.SocketMsgInfo info) {
        this.info = info;
    }
}
