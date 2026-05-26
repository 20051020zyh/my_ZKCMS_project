package com.heima.big_event.service.impl.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.system.SysConfigMapper;
import com.heima.big_event.pojo.SysConfig;
import com.heima.big_event.service.system.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public boolean getStatusImpl(){
        String value = sysConfigMapper.getValueByKey("site_maintenance");
        return "1".equals(value);
    }
}

