package com.heima.big_event.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.SysConfig;

/**
 * 系统全局配置表(SysConfig)表服务接口
 *
 * @author makejava
 * @since 2026-05-14 16:09:00
 */
public interface SysConfigService extends IService<SysConfig> {

    boolean getStatusImpl();
}

