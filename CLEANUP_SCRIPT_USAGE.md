# 🚀 代码清理脚本使用说明

## 📋 脚本功能

`cleanup-redundant-code.ps1` 自动化清理脚本将执行以下操作：

### ✅ 自动删除（7个文件）
1. `AdminServiceImpl.java` (旧版本)
2. `MerchantService.java` (旧接口)
3. `MerchantServiceImpl.java` (旧实现)
4. `MerchantManageController.java`
5. `MerchantQueryController.java`
6. `SimpleOrderService.java`
7. `SimpleOrderServiceImpl.java`

### ✅ 自动删除（2个目录）
1. `user-service/controller/admin/` (整个目录)
2. `product-service/backup/` (整个目录)

### ✅ 自动重命名（3个文件）
1. `AdminServiceImplNew.java` → `AdminServiceImpl.java`
2. `MerchantServiceStandard.java` → `MerchantService.java`
3. `MerchantServiceImplStandard.java` → `MerchantServiceImpl.java`

---

## 🎯 执行步骤

### 方法1：直接运行（推荐）

```powershell
# 1. 打开 PowerShell（以管理员身份运行）
cd D:\Download\Code\sofware\cloud

# 2. 如果是首次运行PowerShell脚本，可能需要设置执行策略
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 3. 执行清理脚本
.\cleanup-redundant-code.ps1
```

### 方法2：逐步执行

```powershell
# 1. 查看脚本内容（可选）
Get-Content .\cleanup-redundant-code.ps1

# 2. 执行脚本
.\cleanup-redundant-code.ps1
```

---

## 🔍 脚本执行流程

### 阶段 1: Git 状态检查
- ✅ 检查是否有未提交的更改
- ✅ 创建清理分支 `feature/code-cleanup`
- ⚠️ 如有未提交更改，会询问是否继续

### 阶段 2: 删除冗余文件
- ✅ 删除旧版 Service 实现
- ✅ 删除旧版 Controller
- ✅ 删除测试代码
- ✅ 删除备份目录

### 阶段 3: 重命名标准化文件
- ✅ 将新标准化文件重命名为正式文件名
- ✅ 检查目标文件是否已存在
- ✅ 记录所有重命名操作

### 阶段 4: 生成报告
- ✅ 生成详细的清理报告
- ✅ 统计删除和重命名数量
- ✅ 记录所有错误（如果有）

### 阶段 5: Git 提交（可选）
- ❓ 询问是否提交更改
- ✅ 自动生成提交信息
- ✅ 提供推送命令

---

## 📄 生成的文件

执行脚本后会生成以下文件：

### 1. `cleanup-report.txt`
详细的清理报告，包含：
- 执行时间
- 删除的文件列表
- 重命名的文件列表
- 统计信息
- 错误信息（如果有）

### 2. `cleanup-backup-log.txt`
备份日志，记录所有操作，用于回滚参考：
- 删除的文件路径和时间
- 重命名的文件路径和时间

---

## ⚠️ 重要注意事项

### 执行前
1. **确保已备份代码**（Git 仓库）
2. **关闭 IDE**（避免文件锁定）
3. **确保没有正在运行的服务**
4. **检查是否有未提交的重要更改**

### 执行后
1. **查看清理报告** (`cleanup-report.txt`)
2. **运行测试** (`mvn clean test`)
3. **检查编译错误** (`mvn clean compile`)
4. **更新引用**（如果IDE报错）

---

## 🔄 回滚方法

### 如果已提交到 Git

```powershell
# 方法1: 重置到上一个提交
git reset --hard HEAD~1

# 方法2: 撤销最后一次提交（保留更改）
git reset --soft HEAD~1

# 方法3: 创建反向提交
git revert HEAD
```

### 如果未提交到 Git

```powershell
# 放弃所有更改
git checkout .

# 恢复特定文件
git checkout -- path/to/file
```

---

## 🐛 常见问题

### Q1: 脚本执行权限错误
**错误**: `无法加载，因为在此系统上禁止运行脚本`

**解决**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Q2: 文件被占用无法删除
**错误**: `文件正被另一进程使用`

**解决**:
1. 关闭 IDE（IntelliJ IDEA, Eclipse等）
2. 关闭所有 Java 进程
3. 重新运行脚本

### Q3: Git 分支已存在
**提示**: `分支已存在或创建失败，继续使用当前分支`

**说明**: 这是正常的，脚本会继续使用当前分支

### Q4: 某些文件不存在
**提示**: `文件不存在: xxx`

**说明**: 这是正常的，可能文件已被删除或路径不同，脚本会跳过

---

## 📊 预期输出示例

```
============================================
  微服务代码清理自动化脚本
============================================

[阶段 1/4] 检查 Git 状态...
✅ Git 工作区干净
✅ 创建清理分支成功

[阶段 2/4] 删除冗余文件...
  ✅ 删除文件: user-service\...\AdminServiceImpl.java
  ✅ 删除文件: user-service\...\MerchantService.java
  ✅ 删除目录: user-service\...\admin
  ✅ 删除目录: product-service\backup

[阶段 3/4] 重命名标准化文件...
  ✅ 重命名: AdminService 实现类
  ✅ 重命名: MerchantService 接口
  ✅ 重命名: MerchantService 实现类

[阶段 4/4] 生成清理报告...

============================================
  清理完成！
============================================

✅ 删除文件数: 7
✅ 删除目录数: 2
✅ 重命名文件数: 3

📄 详细报告已保存到: cleanup-report.txt
📄 备份日志已保存到: cleanup-backup-log.txt

是否提交更改到 Git？(y/n): y

✅ Git 提交成功

可以使用以下命令推送到远程:
  git push origin feature/code-cleanup

============================================
  后续建议
============================================

1. 运行测试确保功能正常：
   mvn clean test

2. 更新引用这些文件的代码（如果有）

3. 如需回滚，使用以下命令：
   git reset --hard HEAD~1

✅ 清理脚本执行完成！
```

---

## 🎓 最佳实践

### 建议的执行顺序

1. **第一步：查看文档**
   ```powershell
   # 查看清理建议
   cat CODE_CLEANUP_AND_MERGE_RECOMMENDATIONS.md
   ```

2. **第二步：创建备份分支**
   ```powershell
   git checkout -b backup/before-cleanup
   git push origin backup/before-cleanup
   ```

3. **第三步：执行清理脚本**
   ```powershell
   .\cleanup-redundant-code.ps1
   ```

4. **第四步：运行测试**
   ```powershell
   mvn clean test
   ```

5. **第五步：检查并提交**
   ```powershell
   # 查看更改
   git status
   git diff
   
   # 如果一切正常，推送
   git push origin feature/code-cleanup
   ```

---

## 📞 需要帮助？

如果遇到问题，请：

1. 查看 `cleanup-report.txt` 了解详细信息
2. 查看 `cleanup-backup-log.txt` 了解操作记录
3. 查看 `CODE_CLEANUP_AND_MERGE_RECOMMENDATIONS.md` 了解清理建议

---

## ✅ 检查清单

执行前检查：
- [ ] 已阅读本文档
- [ ] 已备份代码（Git提交或创建备份分支）
- [ ] 已关闭 IDE
- [ ] 已停止所有服务

执行后检查：
- [ ] 查看 cleanup-report.txt
- [ ] 运行 `mvn clean compile` 检查编译
- [ ] 运行 `mvn clean test` 检查测试
- [ ] 检查 IDE 是否有错误提示
- [ ] 提交更改到 Git

---

**脚本版本**: 1.0  
**创建时间**: 2025-10-01  
**维护人**: what's up  
**状态**: ✅ 可直接使用
