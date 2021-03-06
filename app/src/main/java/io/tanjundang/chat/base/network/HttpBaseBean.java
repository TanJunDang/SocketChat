package io.tanjundang.chat.base.network;


import io.tanjundang.chat.base.Constants;

/**
 * @Author: TanJunDang
 * @Date: 2017/10/17
 * @Description:
 */

public class HttpBaseBean {
    int status;
    String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    private int getStatus() {
        return status;
    }

    private void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        if (status == Constants.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }
}
