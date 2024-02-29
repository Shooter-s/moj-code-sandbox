package com.shooter.mojcodesandbox.model;

import lombok.Data;

/**
 * ClassName: JudgeInfo
 * Package: com.shooter.mojbackend.model.dto.questionsubmit
 * Description: 判题信息
 *
 * @Author:Shooter
 * @Create 2024/2/24 16:30
 * @Version 1.0
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内存
     */
    private Long memory;

    /**
     * 消耗时间
     */
    private Long time;

}
