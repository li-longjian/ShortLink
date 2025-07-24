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
}
