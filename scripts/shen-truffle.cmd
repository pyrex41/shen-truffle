@echo off
setlocal
set "ROOT=%~dp0.."
set "DIST=%ROOT%\target\shen-truffle"
if defined JAVACMD set "JAVA=%JAVACMD%"
if not defined JAVA if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA set "JAVA=java"

if exist "%DIST%\bin\shen-truffle.exe" (
  "%DIST%\bin\shen-truffle.exe" %*
  exit /b %ERRORLEVEL%
)
if exist "%DIST%\bin\shen-truffle.cmd" (
  call "%DIST%\bin\shen-truffle.cmd" %*
  exit /b %ERRORLEVEL%
)
if exist "%DIST%\shen-truffle.jar" (
  "%JAVA%" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JAVA_OPTS% -jar "%DIST%\shen-truffle.jar" %*
  exit /b %ERRORLEVEL%
)
if exist "%ROOT%\target\shen-truffle.jar" (
  "%JAVA%" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JAVA_OPTS% -jar "%ROOT%\target\shen-truffle.jar" %*
  exit /b %ERRORLEVEL%
)
if exist "%ROOT%\target\classes" (
  "%JAVA%" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JAVA_OPTS% -cp "%ROOT%\target\classes;%ROOT%\target\dependency\*" com.github.ragnard.shen.Shen %*
  exit /b %ERRORLEVEL%
)
echo shen-truffle is not built; run: mvn package 1>&2
exit /b 127
