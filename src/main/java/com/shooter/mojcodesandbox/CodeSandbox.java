package com.shooter.mojcodesandbox;


import com.shooter.mojcodesandbox.model.ExecuteCodeRequest;
import com.shooter.mojcodesandbox.model.ExecuteCodeResponse;

/**
 * ClassName: CodeSandbox
 * Package: com.shooter.mojbackend.judge.codesandbox
 * Description: 代码沙箱
 *
 * @Author:Shooter
 * @Create 2024/2/27 8:41
 * @Version 1.0
 */
public interface CodeSandbox {

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);

}
