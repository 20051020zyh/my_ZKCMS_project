package com.heima.big_event.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class  JwtUtil{
    //通过@value从应用配置文件(application.yml)读取jwt.secret(签名密钥字符串)
    @Value("${jwt.secret}")
    private String secret;

    //通过@value从应用配置文件(application.yml)读取jwt.expiration(令牌有效期,单位毫秒)
    @Value("${jwt.expiration}")
    private Long expiration;

    //生成签名密钥
    //getSigningKey方法将配置的密钥字符串转化为符合HS256算法要求的SecretKey对象
    private SecretKey getSigningKey(){
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);

    }

    //生成JWT令牌
    public String generateToken(Integer userId , String username){
        return Jwts.builder()
                //subject()设置令牌主体,一般填用户名或者用户id
                .subject(username)
                //自定义信息,通常加入用户id和用户名,这样方便浏览器识别,也可以直接提取拿来使用
                .claim("userId" , userId)
                .claim("username" , username)
                //签发时间,也就是令牌生成时间
                .issuedAt(new Date())
                //过期时间,expiration设置基于当前时间和配置expiration的过期时间
                .expiration(new Date(System.currentTimeMillis() + expiration))
                //使用getSigningKey()方法生成的密钥和HS256算法对令牌进行签名,确保完整性防止被纂改
                .signWith(getSigningKey() , Jwts.SIG.HS256)
                //compact()方法的作用是终生成一个三段式的JWT字符串
                .compact();
    }

    //从令牌中获取用户名
    public String getUsernameFromToken(String token){
        return getClaimsFromToken(token).getSubject();
    }

    //从令牌中获取用户id
    public Integer getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId" , Integer.class);
    }


    //验证令牌是否有效(未过期且签名正确)
    public boolean validateToken(String token) {
        try {
            ////Jwts.parser()创建一个JWT解析器构建器,解析JWT令牌的起点
            //这个其实就是getClaimsFromToken方法,只不过没有提取claims,只是验证JWT令牌
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e){
            //打印异常,排查核心原因
            System.out.println("JWT验证失败" + e.getMessage());
            //打印完整的异常堆栈跟踪
            //有助于开发时定位问题根源
            e.printStackTrace();
            return false;
        }
    }



    //作用:使用相同的密钥验证令牌签名,并从令牌的Payload(负载)部分提取出所有的声明(claims)信息
    //声明信息里就有用户名id和用户名,所以次啊会
    private Claims getClaimsFromToken(String token) {
        //Jwts.parser()创建一个JWT解析器构建器,解析JWT令牌的起点
        return Jwts.parser()
                //调用getSigningKey()方法获取之前生成JWT时使用的相同密钥
                //验证Jwt签名的密钥,相同才能被成功解析
                .verifyWith(getSigningKey())
                //根据配置构建最终的JwtParser解析器实例
                //将构建器转换为可用的解析器对象
                .build()
                //解析传入的字符串
                //parseSignedClaims方法会执行:
                //验证令牌格式(是否符合三段式结构)
                //验证签名是否有效(使用上一步设置的密钥)
                //验证令牌是否过期(检查exp声明)
                //将解析结果转换为Jws<Claims>对象(JWS是签名的JWT)
                .parseSignedClaims(token)
                //从解析结果中提取有效载荷(Payload)部分
                //返回claims对象,也就是用户名和用户id
                .getPayload();
    }
}