package com.heima.big_event.controller.system;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.big_event.mapper.system.SysConfigMapper;
import com.heima.big_event.pojo.SysConfig;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.system.SysConfigService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequestMapping("/sysConfig")
public class SysConfigController {
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private SysConfigMapper sysConfigMapper;

    //切换维护模式开关
    @PutMapping("/update")
    @RequirePermission("/sysConfig/update")
    public Result update() {
        //先查询当前的维护状态
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey , "site_maintenance");
        SysConfig sysConfig = sysConfigService.getOne(queryWrapper);
        if (sysConfig == null) {
            return Result.error("维护模式配置项不存在");
        }

        //取当前的反状态
        Integer valueOf = Integer.valueOf(sysConfig.getConfigValue());
        Integer newValue =  valueOf == 0 ? 1 : 0;


        LambdaUpdateWrapper<SysConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(SysConfig::getConfigValue, newValue);
        boolean update = sysConfigService.update(wrapper);
        if (!update){
            //获取当前的维护模式状态
            return Result.error("更新维护模式失败");
        }
        return Result.success("当前维护模式状态为" + newValue);
    }

    //前端获取当前的维护状态
    @GetMapping("/get")
    @RequirePermission("/sysConfig/get")
    public Result getStatus(){
        //如果返回的是1,说明开启了维护模式
        if (sysConfigService.getStatusImpl()){
            return Result.success("系统正在维护中");
        }else {
            return Result.success("维护模式状态: 关");
        }
    }
}
