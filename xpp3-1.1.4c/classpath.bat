@echo off
rem
rem This script set classpath
rem 	Usage: classpath [run|build] [set] [queit]
rem By default it sets CLASSPATH to execute direct java invocations (all included)
rem option build prepares LOCALCLASSPATH to use in build.bat
rem option build prepares LOCALCLASSPATH to use in run.bat
rem by using set CLASSPATH also be set by run|build
rem by using queit no echo of set CLASSOATH well be visible
rem
rem written by Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]

set LOCALCLASSPATH=
for %%i in (lib\junit\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\xmlpull\*.jar) do call lib\ant\lcp.bat %%i

if "%1" == "build" goto build_classpath
if "%1" == "run" goto run_classpath
if "%1" == "clean" goto clean_classpath

REM otherwise set user classpath

set LOCALCLASSPATH=classes.cpr;build\classes;build\samples;build\tests;%LOCALCLASSPATH%
set CLASSPATH=%LOCALCLASSPATH%

if "%1" == "quiet" goto end

echo %CLASSPATH%


goto end

:clean_classpath
set CLASSPATH=
set LOCALCLASSPATH=

if "%2" == "quiet" goto end

echo set CLASSPATH=%CLASSPATH%
echo set LOCALCLASSPATH=%LOCALCLASSPATH%

goto end

:build_classpath
for %%i in (lib\ant\*.jar) do call lib\ant\lcp.bat %%i
if exist %JAVA_HOME%\lib\tools.jar set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\tools.jar

goto extra_args

:run_classpath
set LOCALCLASSPATH=build\api;build\classes;build\samples;build\tests;%LOCALCLASSPATH%

:extra_args


if not "%2" == "set" goto check_echo

set CLASSPATH=%LOCALCLASSPATH%

:check_echo

if "%2" == "quiet" goto end
if "%3" == "quiet" goto end

echo %LOCALCLASSPATH%

goto end

:end
