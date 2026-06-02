CREATE TABLE IF NOT EXISTS `browser_device_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `browser` VARCHAR(50) NOT NULL COMMENT '浏览器类型',
  `device_type` VARCHAR(20) NOT NULL COMMENT '设备类型(PC/Mobile/Tablet)',
  `os` VARCHAR(50) NOT NULL COMMENT '操作系统',
  `count` INT NOT NULL DEFAULT 0 COMMENT '访问次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_stat_date` (`stat_date`),
  INDEX `idx_browser` (`browser`),
  INDEX `idx_device_type` (`device_type`),
  INDEX `idx_os` (`os`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览器设备统计表';
