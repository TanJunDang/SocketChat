package io.tanjundang.chat.base.utils;

import android.util.Log;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import org.json.JSONObject;

import java.io.File;

/**
 * @Author: TanJunDang
 * @Date: 2017/11/27
 * @Description:
 */

public class PhotoUploadTool {
    Configuration config = new Configuration.Builder()
            .chunkSize(512 * 1024)        // 分片上传时，每片的大小。 默认256K
            .putThreshhold(1024 * 1024)   // 启用分片上传阀值。默认512K
            .connectTimeout(10)           // 链接超时。默认10秒
            .useHttps(true)               // 是否使用https上传域名
            .responseTimeout(60)          // 服务器响应超时。默认60秒
            .recorder(null)           // recorder分片上传时，已上传片记录器。默认null
//            .recorder(recorder, keyGen)   // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
            .zone(FixedZone.zone0)        // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
            .build();
    // 重用uploadManager。一般地，只需要创建一个uploadManager对象
    UploadManager uploadManager = new UploadManager(config);

    public static PhotoUploadTool getInstance() {
        return Holder.INSTANCE;
    }

    static class Holder {
        static PhotoUploadTool INSTANCE = new PhotoUploadTool();
    }

    public void upload(String path, final String fileName, String token, final Callback callback) {
        uploadManager.put(path, fileName, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo info, JSONObject res) {
                        StringBuilder url = new StringBuilder("http://oduhh49qe.bkt.clouddn.com/");
                        url.append(fileName);
                        //res包含hash、key等信息，具体字段取决于上传策略的设置
                        if (info.isOK()) {
                            callback.onSuccess(url.toString());
                        } else {
                            //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                            callback.onFailure(info.error);
                        }
                        Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                    }
                }, null);
    }

    public void upload(File file, final String fileName, String token, final Callback callback) {
        uploadManager.put(file, fileName, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo info, JSONObject res) {
                        //res包含hash、key等信息，具体字段取决于上传策略的设置
                        StringBuilder url = new StringBuilder("http://oduhh49qe.bkt.clouddn.com/");
                        url.append(fileName);
                        if (info.isOK()) {
                            callback.onSuccess(url.toString());
                        } else {
                            //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                            callback.onFailure(info.error);
                        }
                        Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                    }
                }, null);
    }

    public interface Callback {
        void onSuccess(String url);

        void onFailure(String error);
    }

}
