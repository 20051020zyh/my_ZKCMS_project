package com.heima.big_event.controller.user;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.pojo.*;
import com.heima.big_event.pojo.VO.AdminHomeStatsVO;
import com.heima.big_event.pojo.VO.ArticleCenterInfoVO;
import com.heima.big_event.pojo.VO.UserProfileVO;
import com.heima.big_event.pojo.VO.UserWithRolesVO;
import com.heima.big_event.pojo.dto.PasswordDTO;
import com.heima.big_event.pojo.dto.RegisterDTO;
import com.heima.big_event.service.system.SysPermissionService;
import com.heima.big_event.service.system.SysRolePermissionService;
import com.heima.big_event.service.system.SysRoleService;
import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.service.user.UserService;
import com.heima.big_event.utils.*;
import com.heima.big_event.utils.Others.AliOssUtil;
import com.heima.big_event.utils.Others.Md5Util;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "用户管理", description = "用户注册、登录、信息管理等相关接口")
@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysRolePermissionService sysRolePermissionService;
    @Autowired
    private SysPermissionService sysPermissionService;
    @Autowired
    private SysRoleService sysRoleService;

    //注册
    @Operation(summary = "用户注册", description = "新用户注册账号")
    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDTO registerDTO){
            User u =  userService.findByUserName(registerDTO.getUsername());
            if (u == null){
                //说明没有被占用
                //那么可以注册
                userService.register(registerDTO.getUsername(),registerDTO.getPassword());
                return Result.success();//不用返回响应数据
            }else {
                //被占用了
                return Result.error("用户名被占用!");
            }
    }

    //登录
    @Operation(summary = "用户登录", description = "用户登录获取Token")
    @PostMapping("/login")
    public Result<String> login(@Validated @RequestBody RegisterDTO registerDTO){
        //根据用户名查询用户
        User loginUser = userService.findByUserName(registerDTO.getUsername());
        //判断用户是否存在
        if (loginUser == null){
            return Result.error("用户不存在");
        }
        //判断用户的状态
        if (loginUser.getStatus() == 0){
            return Result.error("账号已被禁用,请联系管理员");
        }

        //判断密码是否正确
        //因为数据库里面的密码是加密的,所以只能加密的和加密的对比
        if (!Md5Util.getMD5String(registerDTO.getPassword()).equals(loginUser.getPassword())){
            return Result.error("密码错误");
        }
        //登录成功
        //=============================权限查询开始=============================
        //使用service方法获取权限（已经包含了超级管理员的特殊处理）
        Set<String> permissions = sysUserRoleService.getUserPermissions(loginUser.getId().longValue());

        //=============================权限查询结束=============================

        //生成token
        String token = jwtUtil.generateToken(loginUser.getId(),loginUser.getUsername());

        //存入redis,设置过期时间和JWT一致
//            String redisKey = "login:token:" + token;
//            redisUtil.set(redisKey , loginUser , 86400);
        String key = "login:user:" + loginUser.getId();
        //存ID
//            redisUtil.hput("userid" , "userid" , loginUser.getId());
        //存用户名
        redisUtil.hput(key , "username" , loginUser.getUsername());
        //存token
        redisUtil.hput(key , "token" , token);

        //把权限集合加入到redis
        redisUtil.hput(key , "permission" , JSON.toJSONString(permissions));
        //把status存入进去
        redisUtil.hput(key , "status" , loginUser.getStatus());
        //设置过期时间
        redisUtil.expire(key , 86400);


        return Result.success(token);//直接返回令牌字符串
    }

    //获取用户详细信息
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/userInfo")
    @RequirePermission("user/userInfo")
    public Result<?> userInfo(){
        //根据用户名查询用户
        //从ThreadLocal中获取
        String userName = ThreadLocalUtil.getUserName();
        Integer userId = ThreadLocalUtil.getUserId();
        //存入redis
        String key = "user:" + userName;
        User user = null;
        //如果redis有的话直接返回
        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null){
            //如果拿到的是""空字符,直接返回没有
            if ("".equals(cacheObj.toString().trim())) {
                return Result.error("没有找到对应的内容,id可能不存在");
            }
            //反序列化JSON字符串为User对象
            try {
                user = objectMapper.readValue(cacheObj.toString() , User.class);
                Map<String, Object> result = buildUserResult(user, userId);
                return Result.success(result);
            } catch (JsonProcessingException e) {
                //反序列化失败,打印堆栈信息,继续走数据库查询
                e.printStackTrace();
            }
        }
        //缓存击穿,加互斥锁重建缓存
        String lockKey = "lock:user:" + userName;
        boolean lockSuccess = false;
        try {
            //尝试加锁
            lockSuccess = redisUtil.lock(lockKey , 30);
            if (!lockSuccess){
                Thread.sleep(200);
                return userInfo();
            }

            //抢到锁了再去查一次redis
            Object cacheObjAfterLock = redisUtil.get(key);
            if (cacheObjAfterLock != null){
                if ("".equals(cacheObjAfterLock.toString().trim())) {
                    return Result.error("没有找到对应的内容,id可能不存在");
                }
                try {
                    user = objectMapper.readValue(cacheObjAfterLock.toString() , User.class);
                    Map<String, Object> result = buildUserResult(user, userId);
                    return Result.success(result);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            //只有一个线程到达数据库
            //redis没有就去查数据库
            user = userService.findByUserName(userName);
            //数据库也没有的话
            if (user == null){
                //缓存穿透,把空值也存入redis,过期时间自定义
                redisUtil.set(key , null , 60);
                return Result.error("没有找到对应的内容,用户可能不存在");
            }
            //如果数据库有的话,返回的同时添加进redis
            //添加雪崩
            int expire = 3600 + new Random().nextInt(300);
            //数据库有的话,将User对象序列化为json字符串存入redis
            try {
                String UserJson = objectMapper.writeValueAsString(user);
                redisUtil.set(key , UserJson , expire);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return Result.error("数据处理异常");
            }
        } catch (InterruptedException e){
             return Result.error("系统繁忙,请稍后再试");
        } finally {
            //无论如何都要释放锁
            if (lockSuccess) {
                redisUtil.unlock(lockKey);
            }
        }
        Map<String, Object> result = buildUserResult(user, userId);
        return Result.success(result);
    }

    private Map<String, Object> buildUserResult(User user, Integer userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("email", user.getEmail());
        map.put("userPic", user.getUserPic());
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreateTime());
        map.put("updateTime", user.getUpdateTime());
        // 查询角色名称
        if (userId != null) {
            List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId.longValue());
            if (!roleIds.isEmpty()) {
                LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
                roleWrapper.in(SysRole::getId, roleIds);
                List<SysRole> roles = sysRoleService.list(roleWrapper);
                List<String> roleNames = roles.stream().map(SysRole::getRoleName).collect(Collectors.toList());
                map.put("roles", roleNames);
            } else {
                map.put("roles", Collections.emptyList());
            }
        } else {
            map.put("roles", Collections.emptyList());
        }
        return map;
    }

    //更新用户信息
    @PutMapping("/update")
    @RequirePermission("user/update")
    public Result update(@RequestBody(required = false) User user){
        Integer loginUserId = ThreadLocalUtil.getUserId();
        if (loginUserId == null){
            return Result.error("未登录,请重新的登录");
        }
        if (user == null) {
            return Result.error("请求体为空，请检查请求数据格式");
        }
        System.out.println("接收到更新请求, 用户数据: " + user);

        // 手动校验：更新时只校验有值的字段，不强制全量校验
        String nickname = user.getNickname();
        if (nickname != null && !nickname.trim().isEmpty()) {
            if (!nickname.matches("^\\S{1,10}$")) {
                return Result.error("昵称必须为1~10位非空白字符");
            }
        }
        String email = user.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return Result.error("邮箱格式不正确");
            }
        }
        String newUsername = user.getUsername();
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            // 查出当前用户
            User currentUser = userService.getById(loginUserId);
            if (currentUser == null) {
                return Result.error("用户不存在");
            }
            // 只有用户名确实变了才需要校验唯一性
            if (!newUsername.equals(currentUser.getUsername())) {
                User existUser = userService.findByUserName(newUsername);
                if (existUser != null) {
                    return Result.error("用户名已被占用");
                }
            }
        }

        user.setId(loginUserId);
        try {
            boolean success = userService.updateById(user);
            if (success) {
                // 清理所有相关的Redis缓存
                String userName = ThreadLocalUtil.getUserName();
                redisUtil.delete("user:" + userName);
                if (newUsername != null && !newUsername.trim().isEmpty()) {
                    redisUtil.delete("user:" + newUsername);
                }
                return Result.success("用户信息更新成功");
            } else {
                return Result.error("更新失败，用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新异常：" + e.getMessage());
        }
    }


    //更新用户头像
    @PostMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam("file") MultipartFile file){
        //参数内还有另外一种方法,就是使用DTO和@ResquestBody,如果使用这样,postman里面就要使用json格式进行传输
        //如果是param的话,就直接传递参数就行,param更适合单一,简单的参数

        //更新头像之前肯定已经登录了
        Integer userId = ThreadLocalUtil.getUserId();
        try {
            // 1. 从 MultipartFile里拿到输入流和原始文件名
            InputStream inputStream = file.getInputStream();
            String originalFilename = file.getOriginalFilename();

            // 2. 调用OSS工具类上传
            String avatarUrl = aliOssUtil.uploadFile(inputStream, originalFilename);

            //把oss图片地址存入数据库
            //调用业务层
            
            userService.updateAvatar(userId , avatarUrl);
            //清空redis缓存
            String userName = ThreadLocalUtil.getUserName();
            redisUtil.delete("user:" + userName);


            //返回最新的头像给前端
            return Result.success(avatarUrl);
        } catch (Exception e){
            e.printStackTrace();
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    @PatchMapping("/updatePwd")
    @RequirePermission("user/updatePwd")
    //更新用户密码
    public Result updatePwd(@RequestBody Map<String , String> params , HttpServletRequest request){
        Integer userId = ThreadLocalUtil.getUserId();
        //加入分布式锁: 避免同时多次操作
        String lockKey = "lock:user:updatePwd:" + userId;
        boolean lockSuccess = false;
        try {
            lockSuccess = redisUtil.lock(lockKey , 30);
            if (!lockSuccess) {
                return Result.error("操作频繁,请稍后再试");
            }
            //正常业务
            //参数校验
            //要和前端传递的参数 KEY一摸一样,不能忽略下划线
            String oldPwd = params.get("old_pwd");
            String newPwd = params.get("new_pwd");
            String rePwd = params.get("re_pwd");
            String userName = ThreadLocalUtil.getUserName();

            //调用工具类校验参数
            String validateMsg = PasswordDTO.validateUpdatePwdParams(oldPwd, newPwd, rePwd, userName);
            if (validateMsg != null){
                return Result.error(validateMsg);
            }
            //调用service层
            User loginuser = userService.findByUserName(userName);
            if (!loginuser.getPassword().equals(Md5Util.getMD5String(oldPwd))){
                return Result.error("原密码填写不正确");
            }
            
            //到了这一步,说明都是对的,那么我们就要把新密码更新了
            userService.updatePwd( userId,newPwd);

            //删除之前获取用户详情信息的缓存
            redisUtil.delete("user:" + userName);


            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 和拦截器保持一致，去掉前缀后再存黑名单
                String token = authHeader.substring(7);
                redisUtil.set("blacklist:" + token, "invalid", 86400);
                //加一行日志,检查是否写入成功
                System.out.println("成功加入toen黑名单:" + token);
            }
            //返回result
            return Result.success();
        } finally {
            if (lockSuccess) {
                redisUtil.unlock(lockKey);
            }
        }
    }

    //退出登录
    @GetMapping("/logout")
    @RequirePermission("user/logout")
    public Result logout(HttpServletRequest request){
        //获取请求头里的token
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.startsWith("Bearer ")){
            String token = authorization.substring(7);
            //业务层:加入黑名单+清空本地用户
            userService.logout(authorization);
            return Result.success("退出成功");
        }
        return Result.error("当前未登录");
    }

    //个人中心接口
    @GetMapping("/center/info")
    @RequirePermission("user/center/info")
    public Result getUserCenterInfo(){
        Integer userId = ThreadLocalUtil.getUserId();

        ArticleCenterInfoVO articleCenterInfoVO = userService.UserCenterInfoImpl(userId);
        return Result.success(articleCenterInfoVO);
    }

    //后台首页统计接口
    //加上管理员权限
    @GetMapping("/admin/home/stats")
    @RequirePermission(value = "user/admin/home/stats", checkPermission = false)
    public Result getAdminHomeStats(){
        Integer userId = ThreadLocalUtil.getUserId();
        AdminHomeStatsVO adminHomeStatsVO = userService.AdminHomeStatsImpl();
        return Result.success(adminHomeStatsVO);
    }

    //获取当前用户的权限路径列表(用于前端菜单级权限校验)
    @GetMapping("/admin/permissionPaths")
    @RequirePermission(value = "user/admin/permissionPaths", checkPermission = false)
    public Result getMyPermissionPaths(){
        Integer userId = ThreadLocalUtil.getUserId();
        String key = "login:user:" + userId;
        Object permissionObj = redisUtil.hget(key, "permission");
        if (permissionObj == null){
            return Result.success(new ArrayList<>());
        }
        String permissionsJson = permissionObj.toString();
        if (permissionsJson.isEmpty()){
            return Result.success(new ArrayList<>());
        }
        List<String> list = JSON.parseArray(permissionsJson, String.class);
        return Result.success(list);
    }

    //校验当前用户是否为后台管理员
    @GetMapping("/admin/checkAdmin")
    @RequirePermission(value = "user/admin/checkAdmin", checkPermission = false)
    public Result checkAdmin(){
        Integer userId = ThreadLocalUtil.getUserId();
        boolean isNonAdmin = sysUserRoleService.isNonAdminUser(userId.longValue());
        return Result.success(!isNonAdmin);
    }

    //全用户分页管理
    @GetMapping("/allUserPage")
    @RequirePermission("user/alluserPage")
    public Result allUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword
    ){
        IPage<UserWithRolesVO> userPageList = userService.getUserPageList(pageNum, pageSize, keyword);
        return Result.success(userPageList);
    }

    //用户状态统计（正常/禁用数）
    @GetMapping("/admin/userStatusStats")
    @RequirePermission(value = "user/admin/userStatusStats", checkPermission = false)
    public Result getUserStatusStats(){
        return Result.success(userService.getUserStatusStats());
    }

    //启用/禁用用户账号
    @PostMapping("/updateStatus")
    @RequirePermission("user/updateStatus")
    public Result updateUserStatus(
            @RequestParam Integer userId,
            @RequestParam Integer status
    ){
        if (status != 0 && status!= 1){
            return Result.error("状态值无效");
        }
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        userService.updateById(user);
        //同步更新redis
        redisUtil.hput("login:user:" + userId , "status" , user.getStatus());
        //直接让token失效
        if (status == 0){
            redisUtil.delete("login:user:" + userId);
        }
        return Result.success();

    }

    //注销用户（级联删除该用户所有相关数据）
    @DeleteMapping("/deleteUser")
    @RequirePermission("user/deleteUser")
    public Result deleteUser(@RequestParam Integer userId) {
        if (userId == null || userId <= 0) {
            return Result.error("用户ID不合法");
        }
        // 不能删除自己
        Integer loginUserId = ThreadLocalUtil.getUserId();
        if (loginUserId != null && loginUserId.equals(userId)) {
            return Result.error("不能注销自己的账号");
        }
        userService.deleteUserWithAllData(userId);
        return Result.success("用户已注销，所有相关数据已清除");
    }

    //查看用户主页（包含用户信息、已发布文章列表、关注数、粉丝数、是否已关注等）
    @GetMapping("/profile/{userId}")
    @RequirePermission(value = "/user/profile", checkPermission = false)
    public Result<UserProfileVO> getUserProfile(@PathVariable Integer userId,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer currentUserId = ThreadLocalUtil.getUserId();
        UserProfileVO vo = userService.getUserProfileImpl(userId, currentUserId, pageNum, pageSize);
        return Result.success(vo);
    }
}
