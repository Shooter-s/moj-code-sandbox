package com.shooter.mojcodesandbox.controller;

import com.shooter.mojcodesandbox.JavaNativeCodeSandbox;
import com.shooter.mojcodesandbox.model.ExecuteCodeRequest;
import com.shooter.mojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ClassName: MainController
 * Package: com.shooter.mojcodesandbox.controller
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/2/29 16:28
 * @Version 1.0
 */
@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    public JavaNativeCodeSandbox javaNativeCodeSandbox;

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response){
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null){
            throw new RuntimeException("请求参数异常");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

}
