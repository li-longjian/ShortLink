package org.llj.shortlink.project.common.constant;


public class RedisKey {
    public static final String FULL_SHORT_URL_KEY = "shortlink:fullShortUrl:%s";
    public static final String LOCK_FULL_SHORT_URL_KEY = "shortlink:lock:fullShortUrl:%s";
    public static final String IS_NULL_FULL_SHORT_URL_KEY = "shortlink:isNull:fullShortUrl:%s";
    public static final String Redis_UV_KEY = "shortlink:status:uv:" ;
}
