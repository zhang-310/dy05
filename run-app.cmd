@echo off
REM 根 POM 无 spring-boot 前缀须写全坐标；不可 -am 与 run 同条命令（会对 contract 等执行 run失败）
REM 先 install 依赖到本地 .m2，再仅对 app 执行 run（package 不写入 .m2，run 可能用到旧 SNAPSHOT）
cd /d "%~dp0"
mvn -pl douyin-operations-app -am install -DskipTests
if errorlevel 1 exit /b 1
mvn -pl douyin-operations-app org.springframework.boot:spring-boot-maven-plugin:3.3.7:run %*
