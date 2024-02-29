package com.shooter.mojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.shooter.mojcodesandbox.model.ExecuteCodeRequest;
import com.shooter.mojcodesandbox.model.ExecuteCodeResponse;
import com.shooter.mojcodesandbox.model.ExecuteMessage;
import com.shooter.mojcodesandbox.model.JudgeInfo;
import com.shooter.mojcodesandbox.security.DefaultSecurityManager;
import com.shooter.mojcodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.StreamType.STDERR;

/**
 * ClassName: JavaDockerCodeSandbox
 * Package: com.shooter.mojcodesandbox
 * Description: docker-java 代码沙箱实现(模板方法)
 *
 * @Author:Shooter
 * @Create 2024/2/29 16:14
 * @Version 1.0
 */
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final Long TIME_OUT = 5000L;

    private static final Boolean FIRST_INIT = true;

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 3、创建容器，把文件复制到容器内
        // 3.1 拉取jdk镜像
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion(); // 阻塞等待镜像拉取完毕
            } catch (InterruptedException e) {
                System.out.println("拉取镜像失败");
                throw new RuntimeException(e);
            }
        }
        // 3.2 创建可交互容器(带有配置的容器，例如内存，cpu核数，编译好的)
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app"))); // 上传文件到linux的app目录下
        hostConfig.withMemory(100 * 1000 * 1000L); // 限制内存
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L); // 限制cpu
        CreateContainerResponse createContainerResponse = containerCmd
                .withReadonlyRootfs(true) // 限制，不让往根目录写
                .withNetworkDisabled(true)
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true) // bash创建容器，不让死掉
                .exec();
        String containerId = createContainerResponse.getId();
        // 3.3 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        // 4 启动容器时交互的命令
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for(String inputArgs :inputList){
            StopWatch stopWatch = new StopWatch();
            // 4.1 创建交互时命令(execCreateCmd)
            String[] inputArgsArray = inputArgs.split(" ");// docker exec containerId java -cp /app Main 1 3
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray); // 命令参数
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withCmd(cmdArray) // 交互的命令
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();

            // 收集结果
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null}; // 内部类，使用外部变量，要加final
            final String[] errorMessage = {null};
            long time = 0L;
            final boolean[] timeOut = {true}; // 默认超时
            // 4.3 创建运行命令回调
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                // 在限制时间内执行完毕
                @Override
                public void onComplete() {
                    timeOut[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (STDERR.equals(streamType)) { // 错误输出
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误:" + errorMessage[0]);
                    } else { // 成功输出
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出成功：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 4.4 实时监控内存(运行用户提交的代码时)
            final long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> resultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(resultCallback); // 执行内存监控

            try {
                stopWatch.start();
                // 4.2 运行命令
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close(); // 关闭内存监控
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 4.3记录输出结果
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }
}


