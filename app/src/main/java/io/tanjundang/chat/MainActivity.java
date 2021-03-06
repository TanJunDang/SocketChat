package io.tanjundang.chat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.tanjundang.chat.base.BaseActivity;
import io.tanjundang.chat.base.BaseFragment;
import io.tanjundang.chat.base.Constants;
import io.tanjundang.chat.base.Global;
import io.tanjundang.chat.base.api.BusinessApi;
import io.tanjundang.chat.base.broadcast.MsgReceiver;
import io.tanjundang.chat.base.entity.QiNiuTokenResp;
import io.tanjundang.chat.base.entity.SocketBaseBean;
import io.tanjundang.chat.base.entity.SocketFriendReqResp;
import io.tanjundang.chat.base.entity.SocketMsgResp;
import io.tanjundang.chat.base.entity.SocketOfflineMsg;
import io.tanjundang.chat.base.entity.type.ChatType;
import io.tanjundang.chat.base.event.FinishEvent;
import io.tanjundang.chat.base.event.ReceiveMsgEvent;
import io.tanjundang.chat.base.network.HttpReqTool;
import io.tanjundang.chat.base.network.SocketConnector;
import io.tanjundang.chat.base.utils.DialogTool;
import io.tanjundang.chat.base.utils.Functions;
import io.tanjundang.chat.base.utils.GsonTool;
import io.tanjundang.chat.base.utils.LogTool;
import io.tanjundang.chat.base.utils.RxBus;
import io.tanjundang.chat.base.utils.cache.CacheTool;
import io.tanjundang.chat.me.MeFragment;
import io.tanjundang.chat.talk.TalkFragment;
import io.tanjundang.chat.friends.FriendsFragment;

public class MainActivity extends BaseActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.content)
    FrameLayout content;
    @BindView(R.id.navigation)
    BottomNavigationView navigation;
    @BindView(R.id.container)
    LinearLayout container;

    BaseFragment currentFragment;
    String[] tags = new String[]{"TalkFragment", "FriendsFragment", "MeFragment"};

    TalkFragment talkFragment;
    FriendsFragment friendsFragment;
    MeFragment meFragment;

    public static SocketConnector connector;
    int ipPort = 9587;
    String ipHost = "39.104.75.186";
    ArrayList<SocketMsgResp.SocketMsgInfo> loadList = new ArrayList<>();

    Disposable closeDisposable;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            SocketFriendReqResp.FriendReqInfo info = (SocketFriendReqResp.FriendReqInfo) msg.getData().getSerializable(Constants.MSG);

            if (info.getType().equals("addFriend")) {
                DialogTool
                        .getInstance()
                        .showSingleBtnDialog(MainActivity.this,
                                "好友请求", "用户：" + info.getName() + "\n内容：" + info.getContent(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
            } else {
                String strMsg = info.isAddFriendSuccess() ? info.getName() + "同意了你的请求" : info.getName() + "拒绝了你的请求" + "\n理由：" + info.getContent();
                DialogTool
                        .getInstance()
                        .showSingleBtnDialog(MainActivity.this,
                                "好友请求结果", strMsg, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Functions.setImmerseStatusBar(this, container);
        navigation.setOnNavigationItemSelectedListener(this);

        initFragment();
        switchContent(currentFragment, talkFragment, 0);
        connector = new SocketConnector(ipHost, ipPort, new SocketConnector.Callback() {
            @Override
            public void receive(String data) {
                LogTool.i(TAG, "收到的消息：" + data);
                if (data.startsWith("\\")) {
                    data = Functions.unicode2String(data);
                }

                try {

                    SocketBaseBean resp = GsonTool.getServerBean(data, SocketBaseBean.class);
                    if (resp.getCode().equals("msg")) {
                        SocketMsgResp bean = GsonTool.getServerBean(data, SocketMsgResp.class);
                        SocketMsgResp.SocketMsgInfo info = bean.getData();
                        loadList.clear();
                        if (info.getGroupId() == 0) {
                            //私聊
                            loadList.addAll(CacheTool.loadReceiveMsg(MainActivity.this, info.getUserId()));
                            loadList.add(info);
                            CacheTool.saveReceiveMsg(MainActivity.this, loadList, info.getUserId());
                        } else {
                            //群聊
                            loadList.addAll(CacheTool.loadGroupReceiveMsg(MainActivity.this, info.getGroupId()));
                            loadList.add(info);
                            CacheTool.saveGroupReceiveMsg(MainActivity.this, loadList, info.getGroupId());
                        }

                        RxBus.getDefault().post(new ReceiveMsgEvent(info));
                        SocketMsgResp.ContentMsg contentMsg = info.getContent();
//                        发送收到消息的广播
                        Intent intent = new Intent(MsgReceiver.MSG_ACTION);
                        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        intent.putExtra(Constants.DATA, info);
                        sendBroadcast(intent);
                        LogTool.i("SocketMsgInfo：", "from  " + info.getUserName() + " :  " + contentMsg.getBody() + "\n");
                    } else if (resp.getCode().equals("notice")) {
                        SocketFriendReqResp bean = GsonTool.getServerBean(data, SocketFriendReqResp.class);
                        SocketFriendReqResp.FriendReqInfo info = bean.getData();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(Constants.MSG, info);
                        Message message = new Message();
                        message.setData(bundle);
                        handler.sendMessage(message);
                    } else if (resp.getCode().equals("response")) {
//                        处理接收到的离线消息
                        try {
                            SocketOfflineMsg bean = GsonTool.getServerBean(data, SocketOfflineMsg.class);
                            for (String json : bean.getData()) {
                                SocketMsgResp itemBean = GsonTool.getServerBean(json, SocketMsgResp.class);
                                SocketMsgResp.SocketMsgInfo info = itemBean.getData();
                                loadList.clear();
                                if (info.getGroupId() == 0 && info.getChatType().equals("p2p")) {
                                    //私聊
                                    loadList.addAll(CacheTool.loadReceiveMsg(MainActivity.this, info.getUserId()));
                                    loadList.add(info);
                                    CacheTool.saveReceiveMsg(MainActivity.this, loadList, info.getUserId());
                                } else {
                                    //群聊
                                    loadList.addAll(CacheTool.loadGroupReceiveMsg(MainActivity.this, info.getGroupId()));
                                    loadList.add(info);
                                    CacheTool.saveGroupReceiveMsg(MainActivity.this, loadList, info.getGroupId());
                                }

                                RxBus.getDefault().post(new ReceiveMsgEvent(info));
                                SocketMsgResp.ContentMsg contentMsg = info.getContent();
                                LogTool.i("SocketMsgInfo：", "收到离线消息from  " + info.getUserName() + " :  " + contentMsg.getBody() + "\n");
                            }
                        } catch (Exception e) {
                            LogTool.e(TAG, e.getCause());
                        }

                    }
                } catch (Exception e) {
//                tvMsg.append("from server:" + data + "\n");
                }
            }

            @Override
            public void reconnect() {

            }
        });

        closeDisposable = RxBus.getDefault()
                .toObservable(FinishEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FinishEvent>() {
                    @Override
                    public void accept(FinishEvent finishEvent) throws Exception {
                        finish();
                    }
                });
    }

    private void initFragment() {
        if (talkFragment == null) {
            talkFragment = new TalkFragment();
        }
        if (friendsFragment == null) {
            friendsFragment = new FriendsFragment();
        }
        if (meFragment == null) {
            meFragment = new MeFragment();
        }
    }

    public void switchContent(BaseFragment from, BaseFragment to, int pos) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            currentFragment = to;
            if (!to.isAdded()) {
                transaction
                        .hide(from)
                        .add(R.id.content, to, tags[pos])
                        .commit();
            } else {
                transaction
                        .hide(from)
                        .show(to)
                        .commit();
            }
        } else {
            currentFragment = new TalkFragment();
            transaction
                    .add(R.id.content, currentFragment, tags[0])
                    .commit();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_talk:
                switchContent(currentFragment, talkFragment, 0);
                return true;
            case R.id.navigation_friends:
                switchContent(currentFragment, friendsFragment, 1);
                return true;
            case R.id.navigation_me:
                switchContent(currentFragment, meFragment, 2);
                return true;
        }
        return false;
    }

    long clickTime;

    @Override
    public void onBackPressed() {
        exit();
    }

    private void exit() {
        if ((System.currentTimeMillis() - clickTime) > 2000) {
            Functions.toast("再按一次后退键退出程序");
            clickTime = System.currentTimeMillis();
        } else {
            this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        HttpReqTool.getInstance().createApi(BusinessApi.class)
                .getQiNiuToken().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<QiNiuTokenResp>() {
                    @Override
                    public void accept(QiNiuTokenResp resp) throws Exception {
                        if (resp.isSuccess()) {
                            QiNiuTokenResp.TokenInfo info = resp.getData();
                            Global.getInstance().setQiniuToken(info.getToken());
                        } else {
                            Functions.toast(resp.getMsg());
                        }
                    }
                });
    }
}
