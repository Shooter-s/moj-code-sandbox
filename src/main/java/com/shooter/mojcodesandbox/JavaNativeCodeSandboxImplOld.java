package com.shooter.mojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.shooter.mojcodesandbox.model.ExecuteCodeRequest;
import com.shooter.mojcodesandbox.model.ExecuteCodeResponse;
import com.shooter.mojcodesandbox.model.ExecuteMessage;
import com.shooter.mojcodesandbox.model.JudgeInfo;
import com.shooter.mojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ClassName: JavaNativeCodeSandboxImpl
 * Package: com.shooter.mojcodesandbox
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/2/27 18:00
 * @Version 1.0
 */
public class JavaNativeCodeSandboxImplOld implements CodeSandbox {

    // 存放用户代码的顶级目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    // 存放用户提交代码的java文件
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 运行超时时间
    private static final Long TIME_OUT = 5000L;

    // 黑名单，用户代码中这些命令的，直接禁止
    private static final List<String> blackList = Arrays.asList("Files","exec");

    // 字典树
    private static final WordTree WORD_TREE;

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    public static void main(String[] args) {
        JavaNativeCodeSandboxImplOld javaNativeCodeSandbox = new JavaNativeCodeSandboxImplOld();
        // 读取本地文件的代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .language("java")
                .code(code)
                .inputList(Arrays.asList("1 2", "1 3"))
                .build();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

//        System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        //1、把用户的代码保存成文件
        //拿到用户工作目录
        String userDir = System.getProperty("user.dir");
        //找到存放代码的最外层目录(File.separator 代替 / ，因为不同系统下的分隔符不一样，这种更有通用性)
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 如果存放代码的目录不存在则创建一个目录
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //不同用户提交代码之间所在目录产生隔离
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        // 将代码写入文件中
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        // 校验黑名单代码
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null){ // 用户代码含有黑名单代码
            System.out.println("包含敏感词:" + foundWord);
            return null;
        }
        //2、编译代码，得到class文件(利用Process类)
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd); // 控制台执行命令
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage); // 编译状态信息
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        // 3、执行代码(命令行)
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制(其他线程)
                new Thread(()->{
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },"t1").start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage); // 运行状态信息
                executeMessages.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        // 4 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        long maxTime = 0;
        List<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessages) { // 遍历每编输入用例运行后返回的结果
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessages.size()) {
            executeCodeResponse.setStatus(0);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        // 麻烦，不做了
//        judgeInfo.setMemory();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5 文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    // 6、 错误处理，提高程序健壮性
    public ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 沙箱出现了错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
