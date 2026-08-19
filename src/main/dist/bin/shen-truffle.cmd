@echo off
setlocal
set "APP=%~dp0.."
if defined JAVACMD (set "JAVA=%JAVACMD%") else if defined JAVA_HOME (set "JAVA=%JAVA_HOME%\bin\java.exe") else (set "JAVA=java")
"%JAVA%" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JAVA_OPTS% -jar "%APP%\lib\shen-truffle.jar" %*
