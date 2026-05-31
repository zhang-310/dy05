@echo off
chcp 65001 >nul
set ROLE=%1
if "%ROLE%"=="" set ROLE=platform
echo Building dy05 for: %ROLE%

call mvn clean package -DskipTests -q -pl douyin-operations-app -am
copy douyin-operations-app\target\douyin-operations-app-*.jar target\%ROLE%-service.jar

echo Done: target\%ROLE%-service.jar
echo Run: java -jar target\%ROLE%-service.jar --dy05.service.role=%ROLE%
