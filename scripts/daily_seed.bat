@echo off
chcp 65001 >nul
echo ========================================
echo  每日数据采集脚本 - %date% %time%
echo ========================================

REM 切换到项目目录
cd /d E:\heimaspringboot\big_event

REM 1. 运行爬虫（采集新文章，使用每日模式）
echo.
echo [1/3] 运行爬虫采集新文章（每日模式）...
python scripts/spider/spider.py daily

REM 2. 导入新文章到数据库
echo.
echo [2/3] 导入文章到数据库...
call mvn test -Dtest=ArticleDataSeeder#seedArticles -q

REM 3. 生成互动数据（可选）
echo.
echo [3/3] 生成点赞/收藏数据...
call mvn test -Dtest=InteractionDataSeeder#seedInteractions -q

echo.
echo ========================================
echo  每日采集完成！
echo ========================================
pause
