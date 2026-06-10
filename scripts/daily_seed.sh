#!/bin/bash
# 每日数据采集脚本
# 用法: bash daily_seed.sh [daily|full]
# 定时任务: crontab -e → 0 20 * * * /opt/big_event/scripts/daily_seed.sh daily

echo "========================================"
echo " 每日数据采集 - $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

cd /opt/big_event

# 模式参数（默认daily）
MODE=${1:-daily}

# 1. 运行爬虫
echo ""
echo "[1/3] 运行爬虫采集新文章（${MODE}模式）..."
python3 scripts/spider/spider.py ${MODE}

if [ $? -ne 0 ]; then
    echo "[错误] 爬虫执行失败，请检查日志"
    exit 1
fi

# 2. 导入文章到数据库
echo ""
echo "[2/3] 导入文章到数据库..."
mvn test -Dtest=ArticleDataSeeder#seedArticles -Dspring.profiles.active=prod -q

if [ $? -ne 0 ]; then
    echo "[错误] 文章导入失败，请检查日志"
    exit 1
fi

# 3. 生成互动数据
echo ""
echo "[3/3] 生成点赞/收藏数据..."
mvn test -Dtest=InteractionDataSeeder#seedInteractions -Dspring.profiles.active=prod -q

if [ $? -ne 0 ]; then
    echo "[警告] 互动数据生成失败，不影响文章导入"
fi

echo ""
echo "========================================"
echo " 每日采集完成！"
echo "========================================"
