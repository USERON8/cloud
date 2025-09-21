@echo off
echo ==========================================
echo GitHub OAuth 编译验证脚本
echo ==========================================
echo.

echo [1/4] 编译 common-module...
cd ..\common-module
call mvn clean compile -q -DskipTests
if %errorlevel% neq 0 (
    echo ❌ common-module 编译失败
    exit /b 1
)
echo ✅ common-module 编译成功

echo.
echo [2/4] 安装 common-module 到本地仓库...
call mvn install -q -DskipTests
if %errorlevel% neq 0 (
    echo ❌ common-module 安装失败
    exit /b 1
)
echo ✅ common-module 安装成功

echo.
echo [3/4] 编译 user-service...
cd ..\user-service
call mvn clean compile -q -DskipTests
if %errorlevel% neq 0 (
    echo ❌ user-service 编译失败
    exit /b 1
)
echo ✅ user-service 编译成功

echo.
echo [4/4] 打包 user-service...
call mvn package -q -DskipTests
if %errorlevel% neq 0 (
    echo ❌ user-service 打包失败
    exit /b 1
)
echo ✅ user-service 打包成功

echo.
echo ==========================================
echo ✅ 所有编译检查都通过！
echo ✅ GitHub OAuth功能可以正常编译和构建
echo ==========================================
echo.
echo 生成的文件：
echo - JAR: %cd%\target\user-service-0.0.1-SNAPSHOT.jar
echo - 大小: 
dir target\user-service-0.0.1-SNAPSHOT.jar | findstr user-service
echo.
pause
