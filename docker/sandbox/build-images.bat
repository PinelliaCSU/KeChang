@echo off
REM =============================================
REM  课畅 - 代码沙箱镜像构建脚本
REM  构建全部语言执行镜像
REM =============================================

echo [1/3] 构建 C 语言执行镜像...
docker build -f docker\sandbox\runner-c.Dockerfile -t kechang/runner-c docker\sandbox\
if %errorlevel% neq 0 exit /b %errorlevel%

echo [2/3] 构建 C++ 执行镜像...
docker build -f docker\sandbox\runner-cpp.Dockerfile -t kechang/runner-cpp docker\sandbox\
if %errorlevel% neq 0 exit /b %errorlevel%

echo [3/3] 构建 Python 执行镜像...
docker build -f docker\sandbox\runner-python.Dockerfile -t kechang/runner-python docker\sandbox\
if %errorlevel% neq 0 exit /b %errorlevel%

echo.
echo ============================================
echo  全部镜像构建完成！列出镜像：
echo ============================================
docker images | findstr "kechang/runner"
