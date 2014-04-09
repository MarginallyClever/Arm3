@echo off

REM $Id: build.bat,v 1.2 2002/04/08 17:49:38 aslom Exp $
REM
REM REQUIRED: Please make sure that JAVA_HOME points to JDK top level dire
REM   and make sure that JAVA_HOME\lib contains tools.jar !!!!
REM 
REM written by Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]

REM JAVA_HOME can be set manualy ..
REM set JAVA_HOME=c:\jdk13

if "%JAVA_HOME%" == "" goto javahomeerror



REM 
REM No need to modify anything after this line.
REM --------------------------------------------------------------------

rem echo JAVA_HOME=%JAVA_HOME%

set OLDCLASSPATH=%CLASSPATH%

call classpath.bat build quiet

set CMD=%JAVA_HOME%\bin\java.exe -classpath %LOCALCLASSPATH% org.apache.tools.ant.Main -buildfile build.xml %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up

set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

rem execute it

echo %CMD%
%CMD%

goto end

REM -----------ERROR-------------
:javahomeerror
echo "ERROR: JAVA_HOME not found in your environment."
echo "Please, set the JAVA_HOME variable in your environment to match the"
echo "location of the Java Virtual Machine you want to use."

:end
