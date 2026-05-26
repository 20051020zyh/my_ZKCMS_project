# API接口连通性测试脚本

$baseUrl = "http://localhost:8081"
$token = ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "开始测试API接口连通性" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 测试1: 获取文章列表（公开接口）
Write-Host "[测试1] 获取文章列表..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/article/pageList?pageNum=1&pageSize=10" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 文章总数: $($response.data.total)" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 测试2: 获取分类列表（公开接口）
Write-Host "[测试2] 获取分类列表..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/category/all/simple" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 分类数量: $($response.data.Count)" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 测试3: 获取热门文章（公开接口）
Write-Host "[测试3] 获取热门文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/article/hot/list?pageNum=1&pageSize=5" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 热门文章数: $($response.data.total)" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 测试4: 获取精选文章（公开接口）
Write-Host "[测试4] 获取精选文章..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/article/best/list?pageNum=1&pageSize=5" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 精选文章数: $($response.data.total)" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 测试5: 获取文章排行（公开接口）
Write-Host "[测试5] 获取文章排行榜..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/article/rank?type=view&limit=10" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 排行榜数据: $($response.data.Count) 条" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 测试6: 用户登录（需要数据库有测试账号）
Write-Host "[测试6] 用户登录测试..." -ForegroundColor Yellow
Write-Host "提示: 请确保数据库中有用户名为 'test' 密码为 '123456' 的测试账号" -ForegroundColor Gray
try {
    $loginData = @{
        username = "test"
        password = "123456"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/user/login" -Method Post -Body $loginData -ContentType "application/json"
    if ($response.code -eq 0) {
        $token = $response.data
        Write-Host "✓ 成功 - 获取Token: $($token.Substring(0, 20))..." -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
        Write-Host "  提示: 请修改测试账号或使用现有账号" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ 错误 - $_" -ForegroundColor Red
}
Write-Host ""

# 如果有token，测试需要登录的接口
if ($token) {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    # 测试7: 获取用户信息
    Write-Host "[测试7] 获取用户信息..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/user/userInfo" -Method Get -Headers $headers
        if ($response.code -eq 0) {
            Write-Host "✓ 成功 - 用户名: $($response.data.username)" -ForegroundColor Green
        } else {
            Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ 错误 - $_" -ForegroundColor Red
    }
    Write-Host ""
    
    # 测试8: 获取用户文章列表
    Write-Host "[测试8] 获取用户文章列表..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/article/user/list" -Method Get -Headers $headers
        if ($response.code -eq 0) {
            Write-Host "✓ 成功 - 文章数量: $($response.data.Count)" -ForegroundColor Green
        } else {
            Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ 错误 - $_" -ForegroundColor Red
    }
    Write-Host ""
    
    # 测试9: 获取系统配置
    Write-Host "[测试9] 获取系统配置..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/sysConfig/get" -Method Get -Headers $headers
        if ($response.code -eq 0) {
            Write-Host "✓ 成功 - 配置信息已获取" -ForegroundColor Green
        } else {
            Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ 错误 - $_" -ForegroundColor Red
    }
    Write-Host ""
    
    # 测试10: 获取公告列表
    Write-Host "[测试10] 获取公告列表..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/sysNotice/list" -Method Get -Headers $headers
        if ($response.code -eq 0) {
            Write-Host "✓ 成功 - 公告数量: $($response.data.Count)" -ForegroundColor Green
        } else {
            Write-Host "✗ 失败 - $($response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ 错误 - $_" -ForegroundColor Red
    }
    Write-Host ""
} else {
    Write-Host "跳过需要登录的接口测试（因为没有获取到Token）" -ForegroundColor Gray
    Write-Host ""
}

# 测试11: 检查前端代理
Write-Host "[测试11] 检查前端代理配置..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:5173/api/article/pageList?pageNum=1&pageSize=10" -Method Get
    if ($response.code -eq 0) {
        Write-Host "✓ 成功 - 前端代理正常工作" -ForegroundColor Green
    } else {
        Write-Host "✗ 失败 - 前端代理可能未启动" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 错误 - 前端可能未启动在5173端口" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "接口测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
