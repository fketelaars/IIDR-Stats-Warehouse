@echo off

rem Get directory of script running
set SCRIPT_DIR=%~dp0

rem Read conf/CollectCDCStats.properties for Access Server home  
FOR /F "tokens=1,2 delims==" %%G IN (%SCRIPT_DIR%\conf\CollectCDCStats.properties) DO (set %%G=%%H)  

rem Set Java executable
set JAVA=%CDC_AS_HOME%\jre32\jre\bin\java.exe


%JAVA% -classpath %SCRIPT_DIR%\lib\*;%CDC_AS_HOME%\lib\*;%SCRIPT_DIR%\lib\downloaded\* com.ibm.replication.iidr.warehouse.CollectCDCStats %*
