package com.shooter.mojcodesandbox.model;

import lombok.Data;

/**
 * ClassName: ExecuteMessage
 * Package: com.shooter.mojcodesandbox.model
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/2/27 22:20
 * @Version 1.0
 */
@Data
public class ExecuteMessage {

    /**
     * 离开状态码 0-正常
     */
    private Integer exitValue;

    /**
     * 成功信息
     */
    private String message;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 用例耗时
     */
    private Long time;
    /**
     * 内存占用
     */
    private Long memory;

}
