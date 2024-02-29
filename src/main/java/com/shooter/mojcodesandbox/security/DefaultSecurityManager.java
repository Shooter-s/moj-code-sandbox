package com.shooter.mojcodesandbox.security;

import java.security.Permission;

/**
 * ClassName: DefaultSecurityManager
 * Package: com.shooter.mojcodesandbox.security
 * Description: 默认安全管理器
 *
 * @Author:Shooter
 * @Create 2024/2/28 13:16
 * @Version 1.0
 */
public class DefaultSecurityManager extends SecurityManager{

    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        // 打开注释默认对所有权限都限制
//        super.checkPermission(perm);
    }
}
