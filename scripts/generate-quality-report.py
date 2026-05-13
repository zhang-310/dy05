#!/usr/bin/env python3
"""
代码质量报告生成器
解析各工具输出并生成统一 JSON 报告
"""
import json
import os
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any


def parse_checkstyle_report(report_path: str) -> Dict[str, Any]:
    """解析 Checkstyle XML 报告"""
    if not os.path.exists(report_path):
        return {"total": 0, "errors": 0, "warnings": 0, "files": []}

    tree = ET.parse(report_path)
    root = tree.getroot()

    errors = 0
    warnings = 0
    files = []

    for file_elem in root.findall('file'):
        file_name = file_elem.get('name')
        file_errors = []

        for error_elem in file_elem.findall('error'):
            severity = error_elem.get('severity')
            if severity == 'error':
                errors += 1
            else:
                warnings += 1

            file_errors.append({
                'line': error_elem.get('line'),
                'column': error_elem.get('column'),
                'severity': severity,
                'message': error_elem.get('message'),
                'source': error_elem.get('source')
            })

        if file_errors:
            files.append({
                'name': file_name,
                'errors': file_errors
            })

    return {
        'total': errors + warnings,
        'errors': errors,
        'warnings': warnings,
        'files': files
    }


def parse_spotbugs_report(report_path: str) -> Dict[str, Any]:
    """解析 SpotBugs XML 报告"""
    if not os.path.exists(report_path):
        return {"total": 0, "high": 0, "medium": 0, "low": 0, "bugs": []}

    tree = ET.parse(report_path)
    root = tree.getroot()

    high = 0
    medium = 0
    low = 0
    bugs = []

    for bug_elem in root.findall('.//BugInstance'):
        priority = int(bug_elem.get('priority', '3'))

        if priority == 1:
            high += 1
        elif priority == 2:
            medium += 1
        else:
            low += 1

        bugs.append({
            'type': bug_elem.get('type'),
            'category': bug_elem.get('category'),
            'priority': priority,
            'message': bug_elem.find('LongMessage').text if bug_elem.find('LongMessage') is not None else ''
        })

    return {
        'total': high + medium + low,
        'high': high,
        'medium': medium,
        'low': low,
        'bugs': bugs
    }


def parse_pmd_report(report_path: str) -> Dict[str, Any]:
    """解析 PMD XML 报告"""
    if not os.path.exists(report_path):
        return {"total": 0, "violations": []}

    tree = ET.parse(report_path)
    root = tree.getroot()

    violations = []

    for file_elem in root.findall('file'):
        file_name = file_elem.get('name')

        for violation_elem in file_elem.findall('violation'):
            violations.append({
                'file': file_name,
                'line': violation_elem.get('beginline'),
                'priority': violation_elem.get('priority'),
                'rule': violation_elem.get('rule'),
                'message': violation_elem.text.strip() if violation_elem.text else ''
            })

    return {
        'total': len(violations),
        'violations': violations
    }


def parse_eslint_report(report_path: str) -> Dict[str, Any]:
    """解析 ESLint JSON 报告"""
    if not os.path.exists(report_path):
        return {"total": 0, "errors": 0, "warnings": 0, "files": []}

    with open(report_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    errors = 0
    warnings = 0
    files = []

    for file_data in data:
        file_errors = file_data.get('errorCount', 0)
        file_warnings = file_data.get('warningCount', 0)

        errors += file_errors
        warnings += file_warnings

        if file_errors > 0 or file_warnings > 0:
            files.append({
                'name': file_data.get('filePath'),
                'errors': file_errors,
                'warnings': file_warnings,
                'messages': file_data.get('messages', [])
            })

    return {
        'total': errors + warnings,
        'errors': errors,
        'warnings': warnings,
        'files': files
    }


def generate_quality_report(reports_dir: str, output_file: str):
    """生成统一质量报告"""
    date = datetime.now().strftime('%Y-%m-%d')

    report = {
        'date': date,
        'timestamp': datetime.now().isoformat(),
        'backend': {
            'checkstyle': parse_checkstyle_report(f'{reports_dir}/checkstyle-result.xml'),
            'spotbugs': parse_spotbugs_report(f'{reports_dir}/spotbugsXml.xml'),
            'pmd': parse_pmd_report(f'{reports_dir}/pmd.xml')
        },
        'frontend': {
            'eslint': parse_eslint_report(f'{reports_dir}/eslint-report.json'),
            'typescript': {
                'errors': 0  # 需要解析 tsc 输出
            }
        },
        'summary': {
            'totalIssues': 0,
            'criticalIssues': 0,
            'status': 'PASS'
        }
    }

    # 计算总问题数
    backend_total = (
        report['backend']['checkstyle']['total'] +
        report['backend']['spotbugs']['total'] +
        report['backend']['pmd']['total']
    )
    frontend_total = report['frontend']['eslint']['total']

    report['summary']['totalIssues'] = backend_total + frontend_total
    report['summary']['criticalIssues'] = (
        report['backend']['checkstyle']['errors'] +
        report['backend']['spotbugs']['high']
    )

    # 判断状态
    if report['summary']['criticalIssues'] > 0:
        report['summary']['status'] = 'FAIL'
    elif report['summary']['totalIssues'] > 100:
        report['summary']['status'] = 'WARN'

    # 写入 JSON 文件
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print(f"✅ 质量报告已生成: {output_file}")
    print(f"   总问题数: {report['summary']['totalIssues']}")
    print(f"   严重问题: {report['summary']['criticalIssues']}")
    print(f"   状态: {report['summary']['status']}")

    return report


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("用法: python generate-quality-report.py <reports_dir> <output_file>")
        sys.exit(1)

    reports_dir = sys.argv[1]
    output_file = sys.argv[2]

    generate_quality_report(reports_dir, output_file)
