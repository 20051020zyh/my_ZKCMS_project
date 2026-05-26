// 通用请求拦截函数
async function request(url, options = {}) {
    // 1. 获取本地存储的 Token
    const token = localStorage.getItem('token');

    // 2. 设置默认请求头（统一为 JSON 格式，匹配后端返回）
    options.headers = {
        'Content-Type': 'application/json', // 改为 JSON 格式，和后端统一
        ...options.headers,
    };

    // 3. 非登录/注册接口，自动携带 Token 到请求头
    // 修正白名单：匹配实际请求的 /user/login、/user/register
    const whiteList = ['/user/login', '/user/register'];
    if (token && !whiteList.includes(url)) {
        options.headers['Authorization'] = 'Bearer ' + token; // 补充 Bearer 前缀（后端拦截器兼容）
    }

    // 4. 处理 POST/PUT 请求参数（转为 JSON 字符串）
    if (options.method && ['POST', 'PUT', 'PATCH'].includes(options.method.toUpperCase())) {
        if (options.body) {
            options.body = JSON.stringify(options.body); // 改为 JSON 字符串
        }
    }

    try {
        // 5. 发送请求（先捕获网络/跨域/HTTP状态码错误）
        const response = await fetch(url, options);

        // ===== 新增：第一步捕获 HTTP 状态码错误（401/403/404/500 等）=====
        if (!response.ok) {
            let statusErrorMsg = '';
            // 解析服务器返回的错误信息（即使状态码非2xx，也尝试拿后端提示）
            try {
                const errorData = await response.json();
                statusErrorMsg = errorData.msg || errorData.error || '';
            } catch (e) {
                // 后端没返回JSON，用状态文本/状态码
                statusErrorMsg = await response.text() || `HTTP ${response.status} ${response.statusText}`;
            }
            // 按状态码分类提示
            switch (response.status) {
                case 401:
                    throw new Error(`身份验证失败（401）：${statusErrorMsg || 'Token无效/过期/未授权'}`);
                case 403:
                    throw new Error(`权限不足（403）：${statusErrorMsg || '无访问该接口的权限'}`);
                case 404:
                    throw new Error(`接口不存在（404）：${statusErrorMsg || `请求地址 ${url} 不存在`}`);
                case 500:
                    throw new Error(`服务器内部错误（500）：${statusErrorMsg || '后端服务异常，请联系开发人员'}`);
                default:
                    throw new Error(`请求失败（${response.status}）：${statusErrorMsg}`);
            }
        }

        // 兼容后端可能返回非 JSON 的情况
        let result;
        try {
            result = await response.json();
        } catch (e) {
            throw new Error(`后端返回格式错误：非 JSON 数据（响应内容：${await response.text()}`);
        }

        // 6. 统一处理后端错误码（匹配后端实际返回的 code=0 为成功）
        // 关键修正：后端 code=0 是成功，其他值是失败
        if (result.code !== 0) {
            // 未登录/Token 无效（后端拦截器返回的错误）
            const tokenInvalidMsg = ['未提供认证令牌', '认证令牌格式错误', '令牌过期'];
            if (tokenInvalidMsg.includes(result.message)) {
                localStorage.removeItem('token'); // 清除无效 Token
                alert('登录状态已失效，请重新登录');
                // 避免重复跳转登录页
                if (window.location.pathname !== '/login.html') {
                    document.body.classList.add('page-fade-out');
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 400);
                }
                throw new Error(`登录状态失效：${result.message}`); // 明确提示Token问题
            }
            // 其他业务错误（返回后端具体的msg）
            throw new Error(`业务请求失败：${result.message || '未知业务错误'}`);
        }

        return result;
    } catch (error) {
        // ===== 新增：第二层捕获网络/跨域等前端侧错误 =====
        let detailedErrorMsg = '';
        // 识别跨域错误
        if (error.message.includes('CORS') || error.message.includes('Access-Control')) {
            detailedErrorMsg = '跨域访问被拦截：浏览器同源策略阻止请求，请配置后端跨域白名单（添加 Access-Control-Allow-Origin）';
        }
        // 识别网络错误（fetch 网络失败会抛 "Failed to fetch"）
        else if (error.message.includes('Failed to fetch')) {
            detailedErrorMsg = '网络请求失败：请检查网络连接，或确认接口地址（' + url + '）是否可访问,以及确认项目是否启动';
        }
        // 其他错误（保留原始错误信息）
        else {
            detailedErrorMsg = error.message || '未知请求错误';
        }

        // 打印详细错误到控制台（方便调试）
        console.error('请求异常详情:', {
            url,
            options,
            error: error.message,
            detailed: detailedErrorMsg
        });
        // 弹窗展示具体错误（给用户/测试看）
        alert(detailedErrorMsg);
        // 抛出具体错误，让业务层能捕获
        throw new Error(detailedErrorMsg);
    }
}

// 简化 GET/POST 请求封装
export const get = (url, params = {}) => {
    const queryString = new URLSearchParams(params).toString();
    const fullUrl = queryString ? `${url}?${queryString}` : url;
    return request(fullUrl, { method: 'GET' });
};

export const post = (url, data = {}) => {
    return request(url, {
        method: 'POST',
        body: data,
    });
};

window.request = request;
window.get = get;
window.post = post;