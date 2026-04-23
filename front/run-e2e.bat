@echo off
REM E2E 测试运行脚本 (Windows)
REM 提供便捷的测试运行命令

setlocal enabledelayedexpansion

REM 检查是否在正确的目录
if not exist "package.json" (
    if exist "front" (
        cd front
    ) else (
        echo [错误] 找不到 package.json，请在项目根目录或 front 目录下运行
        exit /b 1
    )
)

REM 获取命令参数
set COMMAND=%1
if "%COMMAND%"=="" set COMMAND=all

REM 显示帮助信息
if "%COMMAND%"=="help" (
    echo E2E 测试运行脚本
    echo.
    echo 用法: run-e2e.bat [选项]
    echo.
    echo 选项:
    echo   all              运行所有测试（默认）
    echo   ui               UI 模式运行测试
    echo   debug            调试模式运行测试
    echo   headed           有头模式运行测试
    echo   chromium         仅运行 Chromium 测试
    echo   firefox          仅运行 Firefox 测试
    echo   webkit           仅运行 WebKit 测试
    echo   mobile           运行移动端测试
    echo   autofix          运行自动修复测试
    echo   report           查看测试报告
    echo   clean            清理测试结果
    echo   install          安装依赖
    echo   help             显示此帮助信息
    echo.
    echo 模块测试:
    echo   auth             运行认证模块测试
    echo   live             运行直播模块测试
    echo   shortvideo       运行短视频模块测试
    echo   product          运行商品模块测试
    echo   script           运行话术模块测试
    echo   copy             运行文案模块测试
    echo   agent            运行智能体模块测试
    echo   ai               运行 AI 模块测试
    echo   system           运行系统模块测试
    echo   douyin           运行抖音模块测试
    echo   dashboard        运行仪表盘模块测试
    echo.
    echo 示例:
    echo   run-e2e.bat all              # 运行所有测试
    echo   run-e2e.bat ui               # UI 模式
    echo   run-e2e.bat live             # 仅运行直播模块
    echo   run-e2e.bat chromium         # 仅 Chromium 浏览器
    exit /b 0
)

REM 清理测试结果
if "%COMMAND%"=="clean" (
    echo [信息] 清理测试结果...
    if exist "test-results" rmdir /s /q test-results
    if exist "playwright-report" rmdir /s /q playwright-report
    echo [成功] 清理完成
    exit /b 0
)

REM 安装依赖
if "%COMMAND%"=="install" (
    echo [信息] 安装依赖...
    call npm install
    call npx playwright install
    echo [成功] 依赖安装完成
    exit /b 0
)

REM 查看报告
if "%COMMAND%"=="report" (
    call npx playwright show-report
    exit /b 0
)

REM 检查依赖
echo [信息] 检查依赖...
if not exist "node_modules" (
    echo [警告] node_modules 不存在，正在安装依赖...
    call npm install
)

if not exist "node_modules\@playwright" (
    echo [警告] Playwright 未安装，正在安装...
    call npx playwright install
)

echo [成功] 依赖检查完成
echo.

REM 运行测试
echo [信息] 运行测试: %COMMAND%
echo.

if "%COMMAND%"=="all" (
    call npx playwright test
) else if "%COMMAND%"=="ui" (
    call npx playwright test --ui
) else if "%COMMAND%"=="debug" (
    call npx playwright test --debug
) else if "%COMMAND%"=="headed" (
    call npx playwright test --headed
) else if "%COMMAND%"=="chromium" (
    call npx playwright test --project=chromium
) else if "%COMMAND%"=="firefox" (
    call npx playwright test --project=firefox
) else if "%COMMAND%"=="webkit" (
    call npx playwright test --project=webkit
) else if "%COMMAND%"=="mobile" (
    call npx playwright test --project=mobile-chrome --project=mobile-safari
) else if "%COMMAND%"=="autofix" (
    call npx playwright test e2e/tests/autofix.spec.ts --project=chromium-autofix
) else if "%COMMAND%"=="auth" (
    call npx playwright test e2e/tests/auth.spec.ts
) else if "%COMMAND%"=="live" (
    call npx playwright test e2e/tests/live.spec.ts
) else if "%COMMAND%"=="shortvideo" (
    call npx playwright test e2e/tests/shortvideo.spec.ts
) else if "%COMMAND%"=="product" (
    call npx playwright test e2e/tests/product.spec.ts
) else if "%COMMAND%"=="script" (
    call npx playwright test e2e/tests/script.spec.ts
) else if "%COMMAND%"=="copy" (
    call npx playwright test e2e/tests/copy.spec.ts
) else if "%COMMAND%"=="agent" (
    call npx playwright test e2e/tests/agent.spec.ts
) else if "%COMMAND%"=="ai" (
    call npx playwright test e2e/tests/ai.spec.ts
) else if "%COMMAND%"=="system" (
    call npx playwright test e2e/tests/system.spec.ts
) else if "%COMMAND%"=="douyin" (
    call npx playwright test e2e/tests/douyin.spec.ts
) else if "%COMMAND%"=="dashboard" (
    call npx playwright test e2e/tests/dashboard.spec.ts
) else (
    echo [错误] 未知命令: %COMMAND%
    echo 使用 "run-e2e.bat help" 查看帮助
    exit /b 1
)

REM 显示测试结果
if not "%COMMAND%"=="ui" if not "%COMMAND%"=="debug" (
    echo.
    echo [信息] 测试完成！
    echo.

    if exist "test-results\summary.txt" (
        type test-results\summary.txt
    )

    echo.
    echo [信息] 查看详细报告:
    echo   HTML 报告: npx playwright show-report
    echo   自动修复报告: type test-results\autofix-report.md
    echo   测试总结: type test-results\summary.txt
)

endlocal
