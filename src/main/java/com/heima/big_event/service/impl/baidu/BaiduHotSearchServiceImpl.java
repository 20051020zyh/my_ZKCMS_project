package com.heima.big_event.service.impl.baidu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.pojo.BaiduHotItem;
import com.heima.big_event.service.baidu.BaiduHotSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BaiduHotSearchServiceImpl implements BaiduHotSearchService {

    private static final String BAIDU_API_URL =
            "https://top.baidu.com/api/board?platform=wise&tab=realtime";

    /** Redis Key：存放热搜列表 */
    private static final String REDIS_KEY = "baidu:hot:search";

    /** 取前 10 条 */
    private static final int TOP_N = 10;

    /** 缓存有效期（分钟），略大于刷新周期，防止短暂失败导致数据消失 */
    private static final long CACHE_TTL_MINUTES = 20;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 使用 StringRedisTemplate 手动序列化 JSON，
     * 避免 RedisConfig 的 DefaultTyping.NON_FINAL 导致 List 反序列化异常
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ───────────────────── 刷新缓存 ─────────────────────

    @Override
    public boolean refreshHotSearch() {
        try {
            // 1. 构造请求（加 UA 防止被拒）
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(BAIDU_API_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("百度热搜接口返回异常状态：{}", response.getStatusCode());
                return false;
            }

            // 2. 解析 JSON
            List<BaiduHotItem> items = parseResponse(response.getBody());
            if (items.isEmpty()) {
                log.warn("百度热搜解析结果为空，本次不刷新缓存");
                return false;
            }

            // 3. 序列化为 JSON 字符串存入 Redis（避免 DefaultTyping 问题）
            String json = objectMapper.writeValueAsString(items);
            stringRedisTemplate.delete(REDIS_KEY);
            stringRedisTemplate.opsForValue().set(REDIS_KEY, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            log.info("百度热搜刷新成功，共 {} 条，已写入 Redis（key={}）", items.size(), REDIS_KEY);
            return true;

        } catch (Exception e) {
            log.error("百度热搜刷新失败，保留旧缓存不变", e);
            return false;
        }
    }

    // ───────────────────── 读取缓存 ─────────────────────

    @Override
    public List<BaiduHotItem> getHotSearchList() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (json == null || json.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<BaiduHotItem>>() {});
        } catch (Exception e) {
            log.error("百度热搜缓存反序列化失败", e);
            return Collections.emptyList();
        }
    }

    // ───────────────────── 解析百度响应 ─────────────────────

    /**
     * 百度 wise 接口实际 JSON 结构：
     * <pre>
     * data.cards[0].content[0].content  ← 真正的热搜列表
     * </pre>
     * 每个热搜条目字段：word（标题）、url（链接）、index（排名）、
     * hotTag / newHotName（标签：新/热/沸等）、labelTagName（分类标签）
     */
    private List<BaiduHotItem> parseResponse(String body) {
        List<BaiduHotItem> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode cards = root.path("data").path("cards");
            if (cards.isMissingNode() || !cards.isArray() || cards.isEmpty()) {
                log.warn("百度热搜 JSON 中 cards 节点为空");
                return result;
            }

            // 注意：实际结构是 cards[0].content[0].content（两层 content 嵌套）
            JsonNode outerContent = cards.get(0).path("content");
            if (outerContent.isMissingNode() || !outerContent.isArray() || outerContent.isEmpty()) {
                log.warn("百度热搜 JSON 中 outer content 节点为空");
                return result;
            }

            JsonNode innerContent = outerContent.get(0).path("content");
            if (innerContent.isMissingNode() || !innerContent.isArray()) {
                log.warn("百度热搜 JSON 中 inner content 节点为空");
                return result;
            }

            int count = 0;
            for (JsonNode node : innerContent) {
                if (count >= TOP_N) break;

                BaiduHotItem item = new BaiduHotItem();
                item.setRank(count + 1);
                item.setWord(safeText(node, "word"));
                item.setUrl(safeText(node, "url"));

                // desc 用 labelTagName（如"热议"、"辟谣"）或 newHotName（如"新"、"热"、"沸"）
                String labelTag = safeText(node, "labelTagName");
                String hotName = safeText(node, "newHotName");
                if (!labelTag.isEmpty()) {
                    item.setDesc(labelTag);
                } else if (!hotName.isEmpty()) {
                    item.setDesc(hotName);
                } else {
                    item.setDesc("");
                }

                // 百度 wise 接口没有 hotScore 字段，用基于排名的模拟热度值
                // 排名越靠前热度越高
                int hotBase = 100000 - count * 8000;
                item.setHotScore(String.valueOf(Math.max(hotBase, 10000)));

                result.add(item);
                count++;
            }
        } catch (Exception e) {
            log.error("解析百度热搜 JSON 异常", e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? "" : v.asText();
    }
}
