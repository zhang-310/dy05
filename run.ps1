# 在仓库根执行
# 1) 根 POM 无 spring-boot 前缀，须写全插件坐标
# 2) 不可在同一条命令里对 -am 全 reactor 执行 spring-boot:run（会从 contract 起报无 mainClass）
#    应先 install 依赖到本地 .m2，再仅对 app 模块 run（不加 -am）。
#   若用 package 而非 install，下一步 spring-boot:run 仍可能从 .m2 解析到旧的兄弟模块 SNAPSHOT，
#    与 IDE「直接依赖各模块 target/classes」不一致，表现为脚本启动失败、IDE 运行正常。
Set-Location $PSScriptRoot
chcp 65001 | Out-Null
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
mvn -pl douyin-operations-app -am install -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
mvn -pl douyin-operations-app org.springframework.boot:spring-boot-maven-plugin:3.3.7:run @args
