#!/bin/bash
# 扫描所有后端 API 路径

echo "# 后端 API 路径清单"
echo ""
echo "扫描时间: $(date)"
echo ""

find . -name "*Controller.java" -path "*/src/main/java/*" ! -path "*/target/*" | while read file; do
    # 提取类名
    classname=$(basename "$file" .java)

    # 提取 @RequestMapping 路径
    mapping=$(grep -E "@RequestMapping\(|@RequestMapping\s*\(" "$file" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')

    if [ -n "$mapping" ]; then
        # 提取所有 @PostMapping 和 @GetMapping
        echo "## $classname"
        echo "Base: $mapping"
        echo ""

        grep -E "@PostMapping|@GetMapping|@PutMapping|@DeleteMapping" "$file" | while read line; do
            method=$(echo "$line" | grep -oE "Post|Get|Put|Delete")
            path=$(echo "$line" | sed 's/.*"\([^"]*\)".*/\1/')
            if [ "$path" != "$line" ]; then
                echo "- [$method] $mapping$path"
            fi
        done
        echo ""
    fi
done > backend-api-paths.md

echo "扫描完成，结果保存到 backend-api-paths.md"
