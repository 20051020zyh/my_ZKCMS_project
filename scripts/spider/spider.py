"""
IT之家文章爬虫脚本
====================
数据源：IT之家 (ithome.com)
  - 移动端API: 获取文章列表（9个分类，每页25篇）
  - RSS源: 获取最新60篇（含完整HTML）
  - 桌面端详情页: 获取完整文章内容

输出：按分类存储为 JSON 文件到 scripts/data/ 目录

用法：
    cd scripts/spider
    python spider.py

依赖：
    pip install requests beautifulsoup4
"""

import requests
from bs4 import BeautifulSoup
import xml.etree.ElementTree as ET
import json
import os
import re
import time
import random
import hashlib
from datetime import datetime

# ====================== 配置区 ======================

import sys

# 运行模式：通过命令行参数或环境变量控制
# full = 首次全量采集（默认）
# daily = 每日增量采集
MODE = os.environ.get('SPIDER_MODE', 'full')
if len(sys.argv) > 1:
    MODE = sys.argv[1]

# 请求头
HEADERS_PC = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
}
HEADERS_MOBILE = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36',
}

# IT之家移动端API分类ID
ITHOME_API_CATEGORIES = {
    0: '综合',
    1: 'IT圈',
    2: '手机',
    3: '电脑',
    4: '数码',
    5: '游戏',
    6: '汽车',
    7: '家电',
    8: '科学',
}

# 我们的6个目标分类 + 关键词映射（用于自动分类）
OUR_CATEGORIES = {
    '前端': {
        'keywords': [
            '前端', 'HTML', 'CSS', 'JavaScript', 'TypeScript', 'Vue', 'React', 'Angular',
            '网页', '浏览器', 'Chrome', 'Edge', 'Firefox', 'Safari', 'Web', '网站',
            'UI', '界面', '设计', '交互', '动效', '小程序', '微信小程序', 'H5',
            'Node.js', 'npm', 'Webpack', 'Vite', 'Tailwind', 'Bootstrap',
            '响应式', 'PWA', 'Electron', 'Tauri', '鸿蒙', 'HarmonyOS',
        ],
    },
    '后端': {
        'keywords': [
            '后端', '服务器', 'Java', 'Python', 'Go', 'Rust', 'C++', 'C#',
            'Spring', 'Django', 'Flask', 'API', '微服务', '容器', 'Docker', 'K8s',
            'Kubernetes', 'Linux', 'Nginx', 'Apache', 'Tomcat',
            '数据库', 'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'SQL',
            '编程', '开发', '代码', '算法', '开源', 'GitHub', 'Git',
            '云计算', 'AWS', 'Azure', '阿里云', '腾讯云', '华为云',
            '架构', '框架', '中间件', '消息队列', 'Kafka', 'RabbitMQ',
            '微软', '谷歌', 'Google', '苹果', 'Apple', 'Meta', 'OpenAI',
            '芯片', '处理器', 'CPU', 'GPU', '内存', 'SSD', '存储',
            '网络', '5G', '6G', 'Wi-Fi', 'WiFi', '蓝牙', '协议',
            '安全', '漏洞', '加密', '黑客', '隐私', '数据泄露',
            '软件', '应用', '系统', 'Windows', 'macOS', 'Android', 'iOS',
            '更新', '升级', '发布', '版本', 'Beta', '正式版',
        ],
    },
    '数据库': {
        'keywords': [
            '数据库', 'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'SQL',
            'Oracle', 'SQLite', 'MariaDB', 'NoSQL', '分布式数据库',
            'TiDB', 'OceanBase', 'PolarDB', '数据仓库', '大数据',
            'Hadoop', 'Spark', 'Flink', '数据湖', '数据中台',
            'ETL', '数据分析', 'BI', '报表', '索引', '查询',
        ],
    },
    '人工智能': {
        'keywords': [
            'AI', '人工智能', '机器学习', '深度学习', '神经网络',
            'GPT', 'ChatGPT', 'Claude', 'Gemini', 'LLM', '大模型', '大语言模型',
            'AGI', '生成式', 'AIGC', 'Copilot', '自动驾驶', '机器人',
            '自然语言', 'NLP', '计算机视觉', '图像识别', '语音识别',
            'Transformer', 'BERT', 'Diffusion', 'Stable Diffusion',
            '豆包', '文心', '通义', '讯飞', '智谱', 'Kimi',
            '算力', '训练', '推理', '模型', '参数', '微调',
            '英伟达', 'NVIDIA', 'OpenAI', 'Anthropic',
        ],
    },
    '开发工具': {
        'keywords': [
            'IDE', 'VS Code', 'Visual Studio', 'JetBrains', 'IntelliJ',
            '编辑器', '调试', '测试', 'DevOps', 'CI/CD', 'Jenkins',
            'Git', 'GitHub', 'GitLab', '版本控制', '代码审查',
            '终端', '命令行', 'Shell', 'PowerShell', 'Bash',
            '包管理', 'npm', 'pip', 'Maven', 'Gradle',
            '监控', '日志', '性能', '优化', '压测', '基准测试',
            '低代码', '无代码', '可视化', '建站', 'CMS',
            'Notion', 'Figma', 'Postman', 'Swagger',
        ],
    },
    '阅读': {
        'keywords': [
            '阅读', '书评', '推荐', '排行榜', '榜单', '盘点',
            '评测', '体验', '对比', '横评', '选购',
            '教程', '攻略', '指南', '技巧', '方法', '教程',
            '观点', '评论', '思考', '分析', '趋势', '展望',
            '创业', '融资', 'IPO', '上市', '营收', '财报',
            '行业', '市场', '份额', '销量', '出货量',
            '互联网', '科技', '数码', '智能', '未来',
        ],
    },
}

# 请求间隔（秒）
REQUEST_DELAY_MIN = 0.8
REQUEST_DELAY_MAX = 1.5

# newsid扫描模式延迟（更快）
SCAN_DELAY_MIN = 0.3
SCAN_DELAY_MAX = 0.6

# 每个分类最大文章数（根据模式设置）
if MODE == 'daily':
    MAX_ARTICLES_PER_CATEGORY = 30  # 每日模式：每类30篇
    SCAN_RANGE = 300  # 扫描最近300条（确保能采集到足够文章）
else:
    MAX_ARTICLES_PER_CATEGORY = 30  # 全量模式：每类30篇
    SCAN_RANGE = 500  # 扫描最近500条

# 批量扫描时每次跳过的步长（避免太密集）
SCAN_STEP = 2  # 2=隔一篇扫，加快速度

# 输出目录
DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'data')

# ====================== 工具函数 ======================

def random_delay(scan_mode=False):
    """随机延迟，避免请求过快"""
    if scan_mode:
        delay = random.uniform(SCAN_DELAY_MIN, SCAN_DELAY_MAX)
    else:
        delay = random.uniform(REQUEST_DELAY_MIN, REQUEST_DELAY_MAX)
    time.sleep(delay)


def newsid_to_url(newsid):
    """将newsid转换为桌面版URL"""
    s = str(newsid)
    prefix = s[:-3]
    suffix = s[-3:]
    return f'https://www.ithome.com/0/{prefix}/{suffix}.htm'


def clean_html(html_str):
    """清洗HTML内容"""
    if not html_str:
        return ''
    
    soup = BeautifulSoup(html_str, 'html.parser')
    
    # 移除广告声明段落
    for p in soup.find_all('p', class_='ad-tips'):
        p.decompose()
    
    # 移除投稿信息div
    for div in soup.find_all('div', class_='tougao-user'):
        div.decompose()
    
    # 移除script和style
    for tag in soup.find_all(['script', 'style', 'iframe']):
        tag.decompose()
    
    # 移除所有on*事件属性（防XSS）
    for tag in soup.find_all(True):
        attrs_to_remove = [attr for attr in tag.attrs if attr.startswith('on')]
        for attr in attrs_to_remove:
            del tag[attr]
        
        # 移除data-*属性（清理无用属性，保留src/href）
        attrs_to_remove = [attr for attr in tag.attrs if attr.startswith('data-') and attr not in ('data-src',)]
        for attr in attrs_to_remove:
            del tag[attr]
        
        # 移除class属性（不需要原始样式类）
        if 'class' in tag.attrs:
            del tag['class']
        
        # 移除id属性
        if 'id' in tag.attrs:
            del tag['id']
    
    # 处理图片：确保有src属性
    for img in soup.find_all('img'):
        # 优先使用data-original（懒加载原图）
        if img.get('data-original'):
            img['src'] = img['data-original']
            del img['data-original']
        elif img.get('data-src'):
            img['src'] = img['data-src']
            del img['data-src']
        
        # 给图片添加referrerpolicy（解决防盗链问题）
        img['referrerpolicy'] = 'no-referrer'
        
        # 移除无用的宽高属性（让前端CSS自适应）
        for attr in ['width', 'height', 'w', 'h', 'style']:
            if img.has_attr(attr):
                del img[attr]
    
    # 处理链接：将相对URL转为绝对URL
    for a in soup.find_all('a'):
        href = a.get('href', '')
        if href.startswith('/'):
            a['href'] = 'https://www.ithome.com' + href
        
        # 移除nofollow等无用属性
        for attr in ['rel', 'target', 'data-marks']:
            if a.has_attr(attr):
                del a[attr]
    
    # 移除IT之家广告声明文本
    full_text = str(soup)
    ad_patterns = [
        r'广告声明：文内含有的对外跳转链接.*?IT之家所有文章均包含本声明。',
        r'感谢IT之家网友.*?的线索投递！',
    ]
    for pattern in ad_patterns:
        full_text = re.sub(pattern, '', full_text, flags=re.DOTALL)
    
    # 移除空的<p></p>标签
    soup2 = BeautifulSoup(full_text, 'html.parser')
    for p in soup2.find_all('p'):
        if not p.get_text(strip=True) and not p.find('img'):
            p.decompose()
    
    return str(soup2)


def classify_article(title, content_text):
    """根据标题和内容关键词自动分类"""
    combined = (title + ' ' + content_text[:500]).lower()
    
    scores = {}
    for cat_name, cat_info in OUR_CATEGORIES.items():
        score = 0
        for kw in cat_info['keywords']:
            if kw.lower() in combined:
                score += 1
        scores[cat_name] = score
    
    # 选择得分最高的分类
    best_cat = max(scores, key=scores.get)
    if scores[best_cat] == 0:
        # 没有匹配到任何关键词，默认归入"阅读"
        best_cat = '阅读'
    
    return best_cat


def extract_tags(title, content_text):
    """从标题和内容提取标签（最多3个）"""
    combined = title + ' ' + content_text[:300]
    
    # 常见技术标签候选
    tag_candidates = [
        'AI', '人工智能', 'ChatGPT', '大模型', 'Python', 'Java', 'JavaScript',
        'Vue', 'React', 'Spring', 'Docker', 'Linux', 'MySQL', 'Redis',
        '前端', '后端', '算法', '开源', '安全', '云计算', '大数据',
        'Chrome', 'Windows', 'Android', 'iOS', '鸿蒙', '5G',
        'GitHub', '微软', '谷歌', '苹果', '华为', '小米', '腾讯', '阿里',
        '芯片', 'GPU', 'CPU', 'NVIDIA', 'Intel', 'AMD',
        'Web', 'API', '微服务', 'DevOps', '低代码',
        'GPT', 'LLM', '机器人', '自动驾驶', '区块链',
    ]
    
    found_tags = []
    for tag in tag_candidates:
        if tag.lower() in combined.lower() and tag not in found_tags:
            found_tags.append(tag)
            if len(found_tags) >= 3:
                break
    
    return found_tags


# ====================== 数据采集函数 ======================

def fetch_article_list_api(category_id):
    """从IT之家移动端API获取文章列表"""
    url = f'https://m.ithome.com/api/news/newslistpageget?categoryid={category_id}&page=0'
    try:
        r = requests.get(url, headers=HEADERS_MOBILE, timeout=15)
        r.raise_for_status()
        data = r.json()
        if data.get('Success') == 1:
            return data.get('Result', [])
    except Exception as e:
        print(f"  [WARN] API请求失败 cat={category_id}: {e}")
    return []


def fetch_article_list_rss():
    """从IT之家RSS获取文章列表（含完整HTML）"""
    try:
        r = requests.get('https://www.ithome.com/rss/', headers=HEADERS_PC, timeout=15)
        r.raise_for_status()
        root = ET.fromstring(r.text)
        items = []
        for item in root.findall('.//item'):
            title = item.find('title').text or ''
            link = item.find('link').text or ''
            desc = item.find('description').text or ''
            pub_date = item.find('pubDate').text or ''
            
            # 从link提取newsid
            # link格式: https://www.ithome.com/0/962/619.htm
            newsid_match = re.search(r'/0/(\d+)/(\d+)\.htm', link)
            if newsid_match:
                newsid = int(newsid_match.group(1) + newsid_match.group(2))
            else:
                continue
            
            items.append({
                'newsid': newsid,
                'title': title,
                'description': desc,  # RSS中的description就是完整HTML
                'image': '',  # 从HTML中提取
                'postdate': pub_date,
                'from_rss': True,
            })
        return items
    except Exception as e:
        print(f"  [WARN] RSS请求失败: {e}")
    return []


def fetch_article_detail(newsid):
    """从桌面版详情页获取文章完整内容"""
    url = newsid_to_url(newsid)
    try:
        r = requests.get(url, headers=HEADERS_PC, timeout=15)
        r.raise_for_status()
        r.encoding = 'utf-8'
        soup = BeautifulSoup(r.text, 'html.parser')
        
        # 获取标题
        title_tag = soup.select_one('h1')
        title = title_tag.get_text(strip=True) if title_tag else ''
        
        # 获取内容
        paragraph = soup.select_one('#paragraph')
        if not paragraph:
            return None, None
        
        # 提取封面图（第一张图片）
        cover_img = ''
        first_img = paragraph.find('img')
        if first_img:
            cover_img = first_img.get('data-original') or first_img.get('src') or ''
        
        html_content = str(paragraph)
        return title, html_content, cover_img
        
    except Exception as e:
        print(f"  [WARN] 详情页请求失败 newsid={newsid}: {e}")
        return None, None, None


# ====================== newsid扫描采集 ======================

def get_latest_newsid():
    """从RSS获取最新的newsid"""
    try:
        r = requests.get('https://www.ithome.com/rss/', headers=HEADERS_PC, timeout=15)
        root = ET.fromstring(r.text)
        max_id = 0
        for item in root.findall('.//item'):
            link = item.find('link').text or ''
            m = re.search(r'/0/(\d+)/(\d+)\.htm', link)
            if m:
                nid = int(m.group(1) + m.group(2))
                max_id = max(max_id, nid)
        return max_id
    except:
        return 962600  # fallback


def scan_by_newsid_range(start_id, end_id, step=SCAN_STEP):
    """通过newsid范围扫描获取文章"""
    articles = {}
    total = (end_id - start_id) // step
    scanned = 0
    success = 0
    
    for nid in range(start_id, end_id, step):
        scanned += 1
        prefix = str(nid)[:-3]
        suffix = str(nid)[-3:]
        url = f'https://www.ithome.com/0/{prefix}/{suffix}.htm'
        
        try:
            r = requests.get(url, headers=HEADERS_PC, timeout=10)
            if r.status_code != 200:
                continue
            
            r.encoding = 'utf-8'
            soup = BeautifulSoup(r.text, 'html.parser')
            
            paragraph = soup.select_one('#paragraph')
            if not paragraph:
                continue
            
            title_tag = soup.select_one('h1')
            title = title_tag.get_text(strip=True) if title_tag else ''
            
            if not title:
                continue
            
            # 封面图
            cover_img = ''
            first_img = paragraph.find('img')
            if first_img:
                cover_img = first_img.get('data-original') or first_img.get('src') or ''
            
            html_content = str(paragraph)
            
            articles[nid] = {
                'newsid': nid,
                'title': title,
                'html_content': html_content,
                'cover_img': cover_img,
                'description': '',
                'image': cover_img,
                'postdate': '',
                'from_rss': False,
            }
            success += 1
            
        except Exception as e:
            pass  # 静默跳过失败
        
        if scanned % 50 == 0:
            print(f"    扫描进度: {scanned}/{total}, 成功: {success}")
        
        random_delay(scan_mode=True)
    
    return articles


# ====================== 主流程 ======================

def main():
    print("=" * 60)
    print(f"IT之家文章爬虫 v2.0（{MODE}模式）")
    print(f"输出目录: {DATA_DIR}")
    print(f"目标分类: {list(OUR_CATEGORIES.keys())}")
    print(f"每类上限: {MAX_ARTICLES_PER_CATEGORY} 篇")
    print(f"扫描范围: {SCAN_RANGE} 条")
    print("=" * 60)
    
    os.makedirs(DATA_DIR, exist_ok=True)
    
    # ====== 第一步：收集文章 ======
    print("\n[1/4] 采集文章列表...")
    
    all_articles = {}  # newsid -> article_info
    
    # 1a. 从RSS采集（快速，有完整HTML）
    print("  RSS源...", end=' ')
    rss_items = fetch_article_list_rss()
    for item in rss_items:
        nid = item['newsid']
        all_articles[nid] = item
    print(f"获取 {len(rss_items)} 篇")
    
    # 1b. 从移动端API采集
    for cat_id, cat_name in ITHOME_API_CATEGORIES.items():
        print(f"  API分类 {cat_name}(id={cat_id})...", end=' ')
        items = fetch_article_list_api(cat_id)
        new_count = 0
        for item in items:
            nid = item['newsid']
            if nid not in all_articles:
                all_articles[nid] = {
                    'newsid': nid,
                    'title': item.get('title', ''),
                    'description': item.get('description', ''),
                    'image': item.get('image', ''),
                    'postdate': item.get('postdate', ''),
                    'from_rss': False,
                }
                new_count += 1
        print(f"新增 {new_count} 篇（总 {len(all_articles)} 篇）")
        random_delay()
    
    # 1c. newsid范围扫描（补充更多文章）
    latest_id = get_latest_newsid()
    scan_start = latest_id - SCAN_RANGE
    scan_end = latest_id
    print(f"\n  newsid扫描: 范围 {scan_start}~{scan_end}（约 {SCAN_RANGE} 条）...")
    
    scanned = scan_by_newsid_range(scan_start, scan_end, SCAN_STEP)
    scan_new = 0
    for nid, article in scanned.items():
        if nid not in all_articles:
            all_articles[nid] = article
            scan_new += 1
    print(f"  扫描新增 {scan_new} 篇（总去重后 {len(all_articles)} 篇）")
    
    print(f"\n  共收集到 {len(all_articles)} 篇不重复文章")
    
    # ====== 第二步：获取缺少的文章内容 ======
    need_detail = [nid for nid, a in all_articles.items() if not a.get('html_content') and not a.get('from_rss')]
    print(f"\n[2/4] 获取文章详情（需抓取 {len(need_detail)} 篇，已有 {len(all_articles) - len(need_detail)} 篇）...")
    
    success_count = len(all_articles) - len(need_detail)
    fail_count = 0
    
    for i, nid in enumerate(need_detail):
        article = all_articles[nid]
        title, html_content, cover_img = fetch_article_detail(nid)
        
        if html_content:
            article['html_content'] = html_content
            if title:
                article['title'] = title
            article['cover_img'] = cover_img or article.get('image', '')
            success_count += 1
        else:
            article['html_content'] = None
            fail_count += 1
        
        if (i + 1) % 10 == 0:
            print(f"  [{i+1}/{len(need_detail)}] 成功 {success_count}, 失败 {fail_count}")
        
        random_delay()
    
    print(f"\n  详情获取完成: 成功 {success_count}, 失败 {fail_count}")
    
    # ====== 第三步：清洗、分类、提取标签 ======
    print(f"\n[3/4] 清洗和分类文章...")
    
    categorized = {cat: [] for cat in OUR_CATEGORIES}
    skipped = 0
    
    for nid, article in all_articles.items():
        if not article.get('html_content'):
            skipped += 1
            continue
        
        title = article['title']
        
        # 清洗HTML
        clean_content = clean_html(article['html_content'])
        
        # 获取纯文本（用于分类和标签提取）
        soup_text = BeautifulSoup(clean_content, 'html.parser')
        text_content = soup_text.get_text(strip=True)
        
        # 过滤太短的文章
        if len(text_content) < 100:
            skipped += 1
            continue
        
        # 自动分类
        category = classify_article(title, text_content)
        
        # 检查分类是否已满
        if len(categorized[category]) >= MAX_ARTICLES_PER_CATEGORY:
            # 尝试分到其他相关分类
            alt_scores = {}
            for cat_name in OUR_CATEGORIES:
                if cat_name != category and len(categorized[cat_name]) < MAX_ARTICLES_PER_CATEGORY:
                    score = sum(1 for kw in OUR_CATEGORIES[cat_name]['keywords'] if kw.lower() in (title + ' ' + text_content[:500]).lower())
                    alt_scores[cat_name] = score
            if alt_scores and max(alt_scores.values()) > 0:
                category = max(alt_scores, key=alt_scores.get)
            else:
                # 所有分类都满了或没有匹配，找最不满的分类
                min_cat = min(categorized, key=lambda c: len(categorized[c]))
                if len(categorized[min_cat]) >= MAX_ARTICLES_PER_CATEGORY:
                    skipped += 1
                    continue
                category = min_cat
        
        # 提取标签
        tags = extract_tags(title, text_content)
        
        # 组装文章数据
        article_data = {
            'title': title,
            'content': clean_content,
            'coverImg': article.get('cover_img', ''),
            'summary': text_content[:150],
            'tags': tags,
            'categoryName': category,
            'sourceId': nid,
        }
        
        categorized[category].append(article_data)
    
    print(f"\n  分类统计:")
    total = 0
    for cat, articles in categorized.items():
        count = len(articles)
        total += count
        print(f"    {cat}: {count} 篇")
    print(f"  总计: {total} 篇（跳过 {skipped} 篇）")
    
    # ====== 第四步：保存JSON文件 ======
    print(f"\n[4/4] 保存JSON文件...")
    
    for cat, articles in categorized.items():
        if not articles:
            continue
        
        # 文件名映射
        filename_map = {
            '前端': 'articles_frontend.json',
            '后端': 'articles_backend.json',
            '数据库': 'articles_database.json',
            '人工智能': 'articles_ai.json',
            '开发工具': 'articles_devtools.json',
            '阅读': 'articles_reading.json',
        }
        
        filename = filename_map.get(cat, f'articles_{cat}.json')
        filepath = os.path.join(DATA_DIR, filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(articles, f, ensure_ascii=False, indent=2)
        
        print(f"  {filename}: {len(articles)} 篇 → {filepath}")
    
    # 保存元数据
    meta = {
        'crawl_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'total_articles': total,
        'categories': {cat: len(arts) for cat, arts in categorized.items()},
        'skipped': skipped,
    }
    meta_path = os.path.join(DATA_DIR, 'crawl_meta.json')
    with open(meta_path, 'w', encoding='utf-8') as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    print(f"  crawl_meta.json → {meta_path}")
    
    print(f"\n{'=' * 60}")
    print(f"✅ 爬虫完成！共采集 {total} 篇文章")
    print(f"{'=' * 60}")


if __name__ == '__main__':
    main()
