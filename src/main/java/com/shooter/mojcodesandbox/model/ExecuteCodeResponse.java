package com.shooter.mojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ClassName: ExecuteCodeResponse
 * Package: com.shooter.mojbackend.judge.codesandbox.model
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/2/27 8:47
 * @Version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeResponse {

    private List<String> outputList;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 执行状态 1-正常运行完毕
     */
    private Integer status;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;



}