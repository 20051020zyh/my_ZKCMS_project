package com.heima.big_event.utils;

import ua_parser.Client;
import ua_parser.Parser;

import java.util.HashMap;
import java.util.Map;

public class UserAgentParser {
    
    private static final Parser UA_PARSER = new Parser();
    
    public static Map<String, String> parse(String userAgent) {
        Map<String, String> result = new HashMap<>();
        
        if (userAgent == null || userAgent.isEmpty()) {
            result.put("browser", "Unknown");
            result.put("os", "Unknown");
            result.put("deviceType", "Unknown");
            return result;
        }
        
        try {
            Client client = UA_PARSER.parse(userAgent);
            
            String browser = normalizeBrowser(client.userAgent.family);
            String os = normalizeOs(client.os.family);
            String deviceType = detectDeviceType(userAgent);
            
            result.put("browser", browser);
            result.put("os", os);
            result.put("deviceType", deviceType);
        } catch (Exception e) {
            result.put("browser", "Unknown");
            result.put("os", "Unknown");
            result.put("deviceType", "Unknown");
        }
        
        return result;
    }
    
    private static String normalizeBrowser(String browser) {
        if (browser == null) return "Unknown";
        
        String lower = browser.toLowerCase();
        if (lower.contains("chrome") && !lower.contains("edg")) {
            return "Chrome";
        } else if (lower.contains("firefox")) {
            return "Firefox";
        } else if (lower.contains("safari") && !lower.contains("chrome")) {
            return "Safari";
        } else if (lower.contains("edg")) {
            return "Edge";
        } else if (lower.contains("opera") || lower.contains("opr")) {
            return "Opera";
        } else if (lower.contains("qqbrowser")) {
            return "QQ浏览器";
        } else if (lower.contains("micromessenger")) {
            return "微信内置";
        } else if (lower.contains("ucbrowser")) {
            return "UC浏览器";
        } else if (lower.contains("360")) {
            return "360浏览器";
        } else if (lower.contains("maxthon")) {
            return "傲游浏览器";
        } else if (lower.contains("liebao")) {
            return "猎豹浏览器";
        } else if (lower.contains("2345")) {
            return "2345浏览器";
        } else if (lower.contains("mobile") || lower.contains("android")) {
            return "移动端浏览器";
        }
        
        return browser;
    }
    
    private static String normalizeOs(String os) {
        if (os == null) return "Unknown";
        
        String lower = os.toLowerCase();
        if (lower.contains("windows")) {
            return "Windows";
        } else if (lower.contains("mac os") || lower.contains("macos")) {
            return "macOS";
        } else if (lower.contains("ios") || lower.contains("iphone")) {
            return "iOS";
        } else if (lower.contains("android")) {
            return "Android";
        } else if (lower.contains("linux")) {
            return "Linux";
        } else if (lower.contains("harmony")) {
            return "HarmonyOS";
        }
        
        return os;
    }
    
    private static String detectDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        String ua = userAgent.toLowerCase();
        
        boolean isMobile = ua.contains("mobile") || ua.contains("android");
        boolean isTablet = ua.contains("tablet") || ua.contains("ipad") || 
                          (ua.contains("android") && !ua.contains("mobile"));
        
        if (isTablet) {
            return "Tablet";
        } else if (isMobile) {
            return "Mobile";
        }
        
        return "PC";
    }
}
