package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class BrowserDeviceStatsVO implements Serializable {
    private List<Map<String, Object>> browserStats;
    private List<Map<String, Object>> deviceStats;
    private List<Map<String, Object>> osStats;
    private List<Map<String, Object>> browserOsStats;
}
