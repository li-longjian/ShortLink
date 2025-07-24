package org.llj.shortlink.project.utils;


import jakarta.servlet.http.HttpServletRequest;

public class LinkUtil {

    /**
     * 获取id地址
     * @param request
     * @return
     */
    public static String getIP(HttpServletRequest request) {

            String ipAddress = request.getHeader("X-Forwarded-For");

            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;

    }

    /**
     * 获取请求的操作系统信息
     *
     * @param request HttpServletRequest对象
     * @return 操作系统信息字符串
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "Mac";
        } else if (userAgent.toLowerCase().contains("x11")) {
            return "Unix";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("iphone")) {
            return "iPhone";
        } else {
            return "Unknown";
        }
    }
}
