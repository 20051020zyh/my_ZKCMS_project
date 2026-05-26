package com.heima.big_event.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchUpdateStatusDTO {
    private List<Integer> ids;
    private Integer status;
}
