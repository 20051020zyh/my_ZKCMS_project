# 前后端接口连通性测试脚本
# 作者: AI Assistant
# 日期: 2026-05-20

Write-Host "=== 前后端接口连通性测试 ===" -ForegroundColor Cyan
Write-Host "测试时间: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# 配置
$baseUrl = "http://localhost:8081"
$apiPrefix = "/api"  # 前端代理前缀
$testResults = @()

# 颜色函数
function Write-Result {
    param(
        [string]$api,
        [string]$method,
        [int]$statusCode,
        [string]$response,
        [bool]$success
    )
    
    $color = if ($success) { "Green" } else { "Red" }
    $statusText = if ($success) { "✓ 成功" } else { "✗ 失败" }
    
    Write-Host "[$statusText] $method $api" -ForegroundColor $color
    Write-Host "  状态码: $statusCode" -ForegroundColor Gray
    if (-not $success -and $response) {
        Write-Host "  响应: $($response.Substring(0, [Math]::Min(100, $response.Length)))..." -ForegroundColor Yellow
    }
    Write-Host ""
    
    return @{
        Api = "$method $api"
        Success = $success
        StatusCode = $statusCode
    }
}

# 测试函数
function Test-Api {
    param(
        [string]$path,
        [string]$method = "GET",
        [hashtable]$headers = @{},
        [object]$body = $null,
        [string]$description = ""
    )
    
    $url = "$baseUrl$path"
    
    try {
        $params = @{
            Uri = $url
            Method = $method
            TimeoutSec = 10
            ErrorAction = Stop
        }
        
        if ($headers.Count -gt 0) {
            $params.Headers = $headers
        }
        
        if ($body) {
            $params.Body = ($body | ConvertTo-Json -Depth 10)
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-RestMethod @params -ResponseVariable responseVar
        $statusCode = 200
        
        $result = Write-Result -api $path -method $method -statusCode $statusCode -response "成功" -success $true
        $testResults += $result
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $response = $_.ErrorDetails.Message
        
        $result = Write-Result -api $path -method $method -statusCode $statusCode -response $response -success ($statusCode -lt 400)
        $testResults += $result
    }
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "1. 用户模块接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

# 用户注册（测试用）
Test-Api -path "/user/register" -method "POST" -body @{
    username = "testuser_$(Get-Random)"
    password = "Test123456!"
    email = "test$(Get-Random)@example.com"
} -description "用户注册"

# 用户登录
Test-Api -path "/user/login" -method "POST" -body @{
    username = "admin"
    password = "123456"
} -description "用户登录"

# 注意：以下接口需要登录token，这里只测试连通性
Test-Api -path "/user/userInfo" -method "GET" -description "获取用户信息（需登录）"
Test-Api -path "/user/center/info" -method "GET" -description "获取个人中心信息（需登录）"

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "2. 文章模块接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

# 公开接口
Test-Api -path "/article/pageList" -method "GET" -description "获取文章列表"
Test-Api -path "/article/hot/list" -method "GET" -description "获取热门文章"
Test-Api -path "/article/best/list" -method "GET" -description "获取精选文章"
Test-Api -path "/article/rank?type=view&limit=10" -method "GET" -description "获取文章排行"
Test-Api -path "/article/search?keyword=测试" -method "GET" -description "搜索文章"

# 需要登录的接口
Test-Api -path "/article/detail?id=1" -method "GET" -description "获取文章详情"
Test-Api -path "/article/like/check?articleId=1" -method "GET" -description "检查点赞状态"
Test-Api -path "/article/collect/check?articleId=1" -method "GET" -description "检查收藏状态"

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "3. 分类模块接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

Test-Api -path "/category/all/simple" -method "GET" -description "获取所有分类"
Test-Api -path "/category/get/user/list" -method "GET" -description "获取用户分类列表（需登录）"

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "4. 评论模块接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

Test-Api -path "/article/comment/list?articleId=1" -method "GET" -description "获取文章评论列表"
Test-Api -path "/comment/like/check?commentId=1" -method "GET" -description "检查评论点赞状态"

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "5. 系统管理接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

Test-Api -path "/sysConfig/get" -method "GET" -description "获取系统配置（需管理员）"
Test-Api -path "/sysNotice/list" -method "GET" -description "获取公告列表"
Test-Api -path "/index/popInfo" -method "GET" -description "获取首页弹窗信息"

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "6. 文件上传接口测试" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

Test-Api -path "/file/upload" -method "POST" -description "文件上传接口（需登录）"

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "测试汇总" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

$successCount = ($testResults | Where-Object { $_.Success }).Count
$failCount = ($testResults | Where-Object { -not $_.Success }).Count
$totalCount = $testResults.Count

Write-Host "总测试数: $totalCount" -ForegroundColor White
Write-Host "成功: $successCount" -ForegroundColor Green
Write-Host "失败: $failCount" -ForegroundColor Red
Write-Host "成功率: $([math]::Round($successCount / $totalCount * 100, 2))%" -ForegroundColor Cyan

Write-Host ""
Write-Host "失败接口列表:" -ForegroundColor Red
$testResults | Where-Object { -not $_.Success } | ForEach-Object {
    Write-Host "  - $($_.Api) (状态码: $($_.StatusCode))" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 测试完成 ===" -ForegroundColor Cyan
