#!/bin/bash

# E2E 测试运行脚本
# 提供便捷的测试运行命令

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ ${1}${NC}"
}

print_success() {
    echo -e "${GREEN}✓ ${1}${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ${1}${NC}"
}

print_error() {
    echo -e "${RED}✗ ${1}${NC}"
}

# 检查依赖
check_dependencies() {
    print_info "检查依赖..."

    if ! command -v node &> /dev/null; then
        print_error "Node.js 未安装"
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        print_error "npm 未安装"
        exit 1
    fi

    if [ ! -d "node_modules" ]; then
        print_warning "node_modules 不存在，正在安装依赖..."
        npm install
    fi

    if [ ! -d "node_modules/@playwright" ]; then
        print_warning "Playwright 未安装，正在安装..."
        npx playwright install
    fi

    print_success "依赖检查完成"
}

# 显示帮助信息
show_help() {
    echo "E2E 测试运行脚本"
    echo ""
    echo "用法: ./run-e2e.sh [选项]"
    echo ""
    echo "选项:"
    echo "  all              运行所有测试（默认）"
    echo "  ui               UI 模式运行测试"
    echo "  debug            调试模式运行测试"
    echo "  headed           有头模式运行测试"
    echo "  chromium         仅运行 Chromium 测试"
    echo "  firefox          仅运行 Firefox 测试"
    echo "  webkit           仅运行 WebKit 测试"
    echo "  mobile           运行移动端测试"
    echo "  autofix          运行自动修复测试"
    echo "  report           查看测试报告"
    echo "  clean            清理测试结果"
    echo "  install          安装依赖"
    echo "  help             显示此帮助信息"
    echo ""
    echo "模块测试:"
    echo "  auth             运行认证模块测试"
    echo "  live             运行直播模块测试"
    echo "  shortvideo       运行短视频模块测试"
    echo "  product          运行商品模块测试"
    echo "  script           运行话术模块测试"
    echo "  copy             运行文案模块测试"
    echo "  agent            运行智能体模块测试"
    echo "  ai               运行 AI 模块测试"
    echo "  system           运行系统模块测试"
    echo "  douyin           运行抖音模块测试"
    echo "  dashboard        运行仪表盘模块测试"
    echo ""
    echo "示例:"
    echo "  ./run-e2e.sh all              # 运行所有测试"
    echo "  ./run-e2e.sh ui               # UI 模式"
    echo "  ./run-e2e.sh live             # 仅运行直播模块"
    echo "  ./run-e2e.sh chromium         # 仅 Chromium 浏览器"
}

# 清理测试结果
clean_results() {
    print_info "清理测试结果..."
    rm -rf test-results playwright-report
    print_success "清理完成"
}

# 运行测试
run_tests() {
    local command=$1

    print_info "运行测试: ${command}"

    case $command in
        all)
            npx playwright test
            ;;
        ui)
            npx playwright test --ui
            ;;
        debug)
            npx playwright test --debug
            ;;
        headed)
            npx playwright test --headed
            ;;
        chromium)
            npx playwright test --project=chromium
            ;;
        firefox)
            npx playwright test --project=firefox
            ;;
        webkit)
            npx playwright test --project=webkit
            ;;
        mobile)
            npx playwright test --project=mobile-chrome --project=mobile-safari
            ;;
        autofix)
            npx playwright test e2e/tests/autofix.spec.ts --project=chromium-autofix
            ;;
        auth)
            npx playwright test e2e/tests/auth.spec.ts
            ;;
        live)
            npx playwright test e2e/tests/live.spec.ts
            ;;
        shortvideo)
            npx playwright test e2e/tests/shortvideo.spec.ts
            ;;
        product)
            npx playwright test e2e/tests/product.spec.ts
            ;;
        script)
            npx playwright test e2e/tests/script.spec.ts
            ;;
        copy)
            npx playwright test e2e/tests/copy.spec.ts
            ;;
        agent)
            npx playwright test e2e/tests/agent.spec.ts
            ;;
        ai)
            npx playwright test e2e/tests/ai.spec.ts
            ;;
        system)
            npx playwright test e2e/tests/system.spec.ts
            ;;
        douyin)
            npx playwright test e2e/tests/douyin.spec.ts
            ;;
        dashboard)
            npx playwright test e2e/tests/dashboard.spec.ts
            ;;
        report)
            npx playwright show-report
            exit 0
            ;;
        clean)
            clean_results
            exit 0
            ;;
        install)
            npm install
            npx playwright install
            print_success "依赖安装完成"
            exit 0
            ;;
        help)
            show_help
            exit 0
            ;;
        *)
            print_error "未知命令: ${command}"
            show_help
            exit 1
            ;;
    esac
}

# 显示测试结果
show_results() {
    echo ""
    print_info "测试完成！"
    echo ""

    if [ -f "test-results/summary.txt" ]; then
        cat test-results/summary.txt
    fi

    echo ""
    print_info "查看详细报告:"
    echo "  HTML 报告: npx playwright show-report"
    echo "  自动修复报告: cat test-results/autofix-report.md"
    echo "  测试总结: cat test-results/summary.txt"
}

# 主函数
main() {
    local command=${1:-all}

    # 切换到 front 目录
    if [ ! -f "package.json" ]; then
        if [ -d "front" ]; then
            cd front
        else
            print_error "找不到 package.json，请在项目根目录或 front 目录下运行"
            exit 1
        fi
    fi

    # 检查依赖
    if [ "$command" != "help" ] && [ "$command" != "clean" ]; then
        check_dependencies
    fi

    # 运行测试
    run_tests "$command"

    # 显示结果
    if [ "$command" != "ui" ] && [ "$command" != "debug" ]; then
        show_results
    fi
}

# 执行主函数
main "$@"
