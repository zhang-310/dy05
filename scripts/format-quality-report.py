#!/usr/bin/env python3
"""
质量报告格式化器
将 JSON 报告转换为人类可读的 Markdown 格式
"""
import json
import sys
from datetime import datetime
from typing import Dict, Any


def format_summary(report: Dict[str, Any]) -> str:
    """格式化摘要部分"""
    summary = report['summary']
    status_emoji = {
        'PASS': '✅',
        'WARN': '⚠️',
        'FAIL': '❌'
    }

    emoji = status_emoji.get(summary['status'], '❓')

    return f"""# 代码质量报告

**日期**: {report['date']}
**状态**: {emoji} {summary['status']}
**总问题数**: {summary['totalIssues']}
**严重问题**: {summary['criticalIssues']}

---

"""


def format_backend_section(backend: Dict[str, Any]) -> str:
    """格式化后端检查部分"""
    checkstyle = backend['checkstyle']
    spotbugs = backend['spotbugs']
    pmd = backend['pmd']

    return f"""## 后端代码质量

### Checkstyle
- **总问题**: {checkstyle['total']}
- **错误**: {checkstyle['errors']}
- **警告**: {checkstyle['warnings']}

### SpotBugs
- **总问题**: {spotbugs['total']}
- **高优先级**: {spotbugs['high']}
- **中优先级**: {spotbugs['medium']}
- **低优先级**: {spotbugs['low']}

### PMD
- **总违规**: {pmd['total']}

"""


def format_frontend_section(frontend: Dict[str, Any]) -> str:
    """格式化前端检查部分"""
    eslint = frontend['eslint']
    typescript = frontend['typescript']

    return f"""## 前端代码质量

### ESLint
- **总问题**: {eslint['total']}
- **错误**: {eslint['errors']}
- **警告**: {eslint['warnings']}

### TypeScript
- **类型错误**: {typescript['errors']}

"""


def format_top_issues(report: Dict[str, Any], limit: int = 10) -> str:
    """格式化 Top 问题列表"""
    issues = []

    # 收集 SpotBugs 高优先级问题
    for bug in report['backend']['spotbugs']['bugs']:
        if bug['priority'] == 1:
            issues.append({
                'severity': 'HIGH',
                'source': 'SpotBugs',
                'type': bug['type'],
                'message': bug['message']
            })

    # 收集 Checkstyle 错误
    for file_data in report['backend']['checkstyle']['files']:
        for error in file_data['errors']:
            if error['severity'] == 'error':
                issues.append({
                    'severity': 'ERROR',
                    'source': 'Checkstyle',
                    'file': file_data['name'],
                    'line': error['line'],
                    'message': error['message']
                })

    # 收集 ESLint 错误
    for file_data in report['frontend']['eslint']['files']:
        for msg in file_data.get('messages', []):
            if msg.get('severity') == 2:  # ESLint error
                issues.append({
                    'severity': 'ERROR',
                    'source': 'ESLint',
                    'file': file_data['name'],
                    'line': msg.get('line'),
                    'message': msg.get('message')
                })

    if not issues:
        return "## Top 问题\n\n✅ 未发现严重问题\n\n"

    # 限制数量
    issues = issues[:limit]

    markdown = "## Top 问题\n\n"
    for i, issue in enumerate(issues, 1):
        markdown += f"### {i}. [{issue['severity']}] {issue['source']}\n"
        if 'file' in issue:
            markdown += f"- **文件**: `{issue['file']}`\n"
        if 'line' in issue:
            markdown += f"- **行号**: {issue['line']}\n"
        if 'type' in issue:
            markdown += f"- **类型**: {issue['type']}\n"
        markdown += f"- **描述**: {issue['message']}\n\n"

    return markdown


def format_recommendations(report: Dict[str, Any]) -> str:
    """生成改进建议"""
    summary = report['summary']
    recommendations = []

    if summary['criticalIssues'] > 0:
        recommendations.append("🔴 **立即修复严重问题**：发现 {} 个严重问题，建议优先处理".format(summary['criticalIssues']))

    if report['backend']['spotbugs']['high'] > 0:
        recommendations.append("⚠️ **SpotBugs 高优先级问题**：发现 {} 个高优先级 bug，可能导致运行时错误".format(
            report['backend']['spotbugs']['high']))

    if report['backend']['checkstyle']['errors'] > 10:
        recommendations.append("📝 **代码风格问题**：Checkstyle 发现 {} 个错误，建议统一代码风格".format(
            report['backend']['checkstyle']['errors']))

    if report['frontend']['eslint']['errors'] > 10:
        recommendations.append("🎨 **前端代码问题**：ESLint 发现 {} 个错误，建议修复".format(
            report['frontend']['eslint']['errors']))

    if not recommendations:
        recommendations.append("✅ **代码质量良好**：未发现严重问题，继续保持")

    markdown = "## 改进建议\n\n"
    for rec in recommendations:
        markdown += f"- {rec}\n"

    return markdown + "\n"


def format_quality_report(json_file: str, output_file: str):
    """将 JSON 报告转换为 Markdown"""
    with open(json_file, 'r', encoding='utf-8') as f:
        report = json.load(f)

    markdown = ""
    markdown += format_summary(report)
    markdown += format_backend_section(report['backend'])
    markdown += format_frontend_section(report['frontend'])
    markdown += format_top_issues(report)
    markdown += format_recommendations(report)

    markdown += f"""---

*报告生成时间: {report['timestamp']}*
"""

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(markdown)

    print(f"✅ Markdown 报告已生成: {output_file}")


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("用法: python format-quality-report.py <json_file> <output_file>")
        sys.exit(1)

    json_file = sys.argv[1]
    output_file = sys.argv[2]

    format_quality_report(json_file, output_file)
