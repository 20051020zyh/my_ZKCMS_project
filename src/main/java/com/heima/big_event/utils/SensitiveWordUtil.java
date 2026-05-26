package com.heima.big_event.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SensitiveWordUtil {
    private static final Map<Character, HashMap> SENSITIVE_MAP = new HashMap<>();
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>();

    static {
        // 这里写你的敏感词
        SENSITIVE_WORDS.add("违规");
        SENSITIVE_WORDS.add("垃圾");
        SENSITIVE_WORDS.add("广告");
        initSensitiveWordMap();
    }

    private static void initSensitiveWordMap() {
        for (String word : SENSITIVE_WORDS) {
            Map<Character, HashMap> currentMap = SENSITIVE_MAP;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (!currentMap.containsKey(c)) {
                    HashMap<Character, HashMap> child = new HashMap<>();
                    currentMap.put(c, child);
                }
                currentMap = currentMap.get(c);
            }
            currentMap.put('*', null);
        }
    }

    public static boolean containsSensitive(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (checkWord(text, i)) return true;
        }
        return false;
    }

    private static boolean checkWord(String text, int begin) {
        Map<Character, HashMap> currentMap = SENSITIVE_MAP;
        for (int i = begin; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!currentMap.containsKey(c)) break;
            currentMap = currentMap.get(c);
            if (currentMap.containsKey('*')) return true;
        }
        return false;
    }
}
