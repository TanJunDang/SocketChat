package io.tanjundang.chat.base.utils.cache;

import android.content.Context;

/**
 * @Author: TanJunDang
 * @Date: 2017/6/19
 * @Description: 缓存工具 CacheModel调用
 */

public class CacheTool {
    private static final String ACCOUNT = "ACCOUNT";
    private static final String PROVINCE = "PROVINCE";
    private static final String CITY = "CITY";
    private static final String COUNTY = "COUNTY";
    private static final String CACHE = "CACHE";

    public static void release(Context context) {
        CacheModel.clear(context);
    }

}