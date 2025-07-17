package org.llj.shortlink.admin.common.context;

public class BaseContext {

    public static ThreadLocal<String> threadLocal = new ThreadLocal<>();


    public static void setUserName(String userName) {
        threadLocal.set(userName);
    }

    public static String  getUserName() {
        return threadLocal.get();
    }

    public static void clearUserName() {
        threadLocal.remove();
    }



}
