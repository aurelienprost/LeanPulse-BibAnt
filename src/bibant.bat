@echo off

if "%BIBANT_HOME%"=="" goto setDefaultBibAntHome
goto stripBibAntHome

:setDefaultBibAntHome
rem %~dp0 is expanded pathname of the current script under NT
set BIBANT_HOME=%~dp0

:stripBibAntHome
if not _%BIBANT_HOME:~-1%==_\ goto manageArgs
set BIBANT_HOME=%BIBANT_HOME:~0,-1%
goto stripBibAntHome

:manageArgs
rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set BIBANT_CMD_LINE_ARGS=
:setupArgs
if ""%1""=="""" goto doneStart
set BIBANT_CMD_LINE_ARGS=%BIBANT_CMD_LINE_ARGS% %1
shift
goto setupArgs
rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.
:doneStart

set LIBDIR=%BIBANT_HOME%\lib
set LOCALCLASSPATH=%LIBDIR%\bibant.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\activation-1.1.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-apache-bsf-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-apache-log4j-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-apache-oro-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-apache-regexp-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-apache-resolver-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-jmf-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-jsch-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-launcher-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-swing-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-contrib-1.0b3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\ant-javamail-1.8.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\avalon-framework-api-4.3.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\avalon-framework-impl-4.3.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\batik-all-1.7.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\bcmail-jdk15on-147.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\bcprov-jdk15on-147.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\bsf-2.4.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\bsh-2.0b4.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\commons-compress-1.3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\commons-io-1.3.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\commons-logging-1.1.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\commons-net-3.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\dom4j-1.6.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\fontbox-1.6.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\fop-1.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jacob-1.16.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jDDE.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\jempbox-1.6.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\js-1.7r3.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\log4j-1.2.16.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\mail-1.4.5.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\metadata-extractor-2.4.0-beta-1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\mysql-connector-java-5.1.19.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\pdfbox-1.6.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\poi-3.8.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\poi-ooxml-3.8.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\poi-ooxml-schemas-3.8.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\poi-scratchpad-3.8.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\saxon-9.1.0.8-dom.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\saxon-9.1.0.8.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\stax-api-1.0.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\tika-core-1.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\tika-parsers-1.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xmlgraphics-commons-1.4.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xmlbeans-2.3.0.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xmltask-1.16.1.jar

set JAVAOPTS=-Xmx1024m
set JAVAOPTS=%JAVAOPTS% -Djavax.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl
set JAVAOPTS=%JAVAOPTS% "-Denv.windir=%WINDIR%"
set JAVAOPTS=%JAVAOPTS% "-Dant.home=%BIBANT_HOME%"
set JAVAOPTS=%JAVAOPTS% "-Djava.library.path=%LIBDIR%"
set JAVAOPTS=%JAVAOPTS% "-Dlog4j.configuration=file:%LIBDIR%\log.properties"
set JAVAOPTS=%JAVAOPTS% -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java
goto runAnt

:noJavaHome
if "%JAVACMD%" == "" set JAVACMD=java

:runAnt
"%JAVACMD%" %JAVAOPTS% -cp "%LOCALCLASSPATH%" com.leanpulse.bibant.Launcher %BIBANT_CMD_LINE_ARGS%