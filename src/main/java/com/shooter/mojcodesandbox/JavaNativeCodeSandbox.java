package com.shooter.mojcodesandbox;

import com.shooter.mojcodesandbox.model.ExecuteCodeRequest;
import com.shooter.mojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * ClassName: JavaNativeCodeSandboxTemplateImpl
 * Package: com.shooter.mojcodesandbox
 * Description: java原生代码沙箱实现(直接复用模板方法)
 *
 * @Author:Shooter
 * @Create 2024/2/29 16:09
 * @Version 1.0
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
