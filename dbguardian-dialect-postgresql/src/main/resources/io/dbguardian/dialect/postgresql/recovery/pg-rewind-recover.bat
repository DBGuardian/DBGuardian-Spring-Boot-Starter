@echo off
setlocal EnableExtensions EnableDelayedExpansion

call :require DBGUARDIAN_OLD_PGDATA || exit /b 1
call :require DBGUARDIAN_NEW_PRIMARY_HOST || exit /b 1
call :require DBGUARDIAN_NEW_PRIMARY_PORT || exit /b 1
call :require DBGUARDIAN_REPL_USER || exit /b 1
call :require DBGUARDIAN_REPL_PASS || exit /b 1
call :require DBGUARDIAN_PG_CTL || exit /b 1
call :require DBGUARDIAN_PSQL || exit /b 1
call :require DBGUARDIAN_PG_REWIND || exit /b 1

set "OLD_PGDATA=%DBGUARDIAN_OLD_PGDATA%"
set "NEW_PRIMARY_HOST=%DBGUARDIAN_NEW_PRIMARY_HOST%"
set "NEW_PRIMARY_PORT=%DBGUARDIAN_NEW_PRIMARY_PORT%"
set "REPL_USER=%DBGUARDIAN_REPL_USER%"
set "REPL_PASS=%DBGUARDIAN_REPL_PASS%"
set "PG_CTL_CMD=%DBGUARDIAN_PG_CTL%"
set "PSQL_CMD=%DBGUARDIAN_PSQL%"
set "PG_REWIND_CMD=%DBGUARDIAN_PG_REWIND%"
set "LOCAL_HOST=%DBGUARDIAN_LOCAL_HOST%"
if not defined LOCAL_HOST set "LOCAL_HOST=127.0.0.1"
set "LOCAL_PORT=%DBGUARDIAN_LOCAL_PORT%"
if not defined LOCAL_PORT set "LOCAL_PORT=5432"
set "LOCAL_DB=%DBGUARDIAN_LOCAL_DB%"
if not defined LOCAL_DB set "LOCAL_DB=postgres"
set "LOCAL_USER=%DBGUARDIAN_LOCAL_USER%"
if not defined LOCAL_USER set "LOCAL_USER=%REPL_USER%"
set "LOCAL_PASS=%DBGUARDIAN_LOCAL_PASS%"
if not defined LOCAL_PASS set "LOCAL_PASS=%REPL_PASS%"
set "SLOT_NAME=%DBGUARDIAN_SLOT_NAME%"
set "EXTRA_PRIMARY_CONNINFO=%DBGUARDIAN_EXTRA_PRIMARY_CONNINFO%"
set "STARTUP_WAIT_SECONDS=%DBGUARDIAN_STARTUP_WAIT_SECONDS%"
if not defined STARTUP_WAIT_SECONDS set "STARTUP_WAIT_SECONDS=60"

call :log check new primary status
set "PGPASSWORD=%REPL_PASS%"
for /f "usebackq delims=" %%i in (`"%PSQL_CMD%" -h "%NEW_PRIMARY_HOST%" -p "%NEW_PRIMARY_PORT%" -U "%REPL_USER%" -d postgres -Atqc "select case when pg_is_in_recovery() then 'standby' else 'primary' end"`) do set "PRIMARY_STATE=%%i"
if /i not "%PRIMARY_STATE%"=="primary" call :fail new primary is not writable
call :ensureReplicationSlot
if errorlevel 1 exit /b 1

if not exist "%OLD_PGDATA%" call :fail pgdata not found: %OLD_PGDATA%

call :log stop old primary
"%PG_CTL_CMD%" -D "%OLD_PGDATA%" -m fast stop >nul 2>nul
timeout /t 2 /nobreak >nul

call :log dry-run pg_rewind
"%PG_REWIND_CMD%" -D "%OLD_PGDATA%" --source-server="host=%NEW_PRIMARY_HOST% port=%NEW_PRIMARY_PORT% user=%REPL_USER% dbname=postgres" --dry-run >nul || call :fail pg_rewind dry-run failed

call :log run pg_rewind
"%PG_REWIND_CMD%" -D "%OLD_PGDATA%" --source-server="host=%NEW_PRIMARY_HOST% port=%NEW_PRIMARY_PORT% user=%REPL_USER% dbname=postgres" >nul || call :fail pg_rewind failed

set "AUTO_CONF=%OLD_PGDATA%\postgresql.auto.conf"
>>"%AUTO_CONF%" echo primary_conninfo = 'host=%NEW_PRIMARY_HOST% port=%NEW_PRIMARY_PORT% user=%REPL_USER% password=%REPL_PASS% application_name=%COMPUTERNAME% %EXTRA_PRIMARY_CONNINFO%'
if defined SLOT_NAME >>"%AUTO_CONF%" echo primary_slot_name = '%SLOT_NAME%'
if not exist "%OLD_PGDATA%\standby.signal" type nul > "%OLD_PGDATA%\standby.signal"

call :log start old primary as standby
"%PG_CTL_CMD%" -D "%OLD_PGDATA%" start >nul || call :fail failed to start old primary

call :log wait recovery mode
set /a WAITED=0
:wait_loop
call :checkLocalRecoveryMode
if "%LOCAL_RECOVERY_MODE%"=="t" (
  call :log old node recovered as standby
  exit /b 0
)
if %WAITED% GEQ %STARTUP_WAIT_SECONDS% call :fail old node did not enter recovery mode
set /a WAITED+=1
timeout /t 1 /nobreak >nul
goto wait_loop

:checkLocalRecoveryMode
set "LOCAL_RECOVERY_MODE="
set "PGPASSWORD=%LOCAL_PASS%"
for /f "usebackq delims=" %%i in (`"%PSQL_CMD%" -h "%LOCAL_HOST%" -p "%LOCAL_PORT%" -U "%LOCAL_USER%" -d "%LOCAL_DB%" -Atqc "select case when pg_is_in_recovery() then 't' else 'f' end" 2^>nul`) do set "LOCAL_RECOVERY_MODE=%%i"
exit /b 0

:ensureReplicationSlot
if not defined SLOT_NAME exit /b 0
call :log ensure replication slot exists: %SLOT_NAME%
set "SLOT_STATE="
set "PGPASSWORD=%REPL_PASS%"
for /f "usebackq delims=" %%i in (`"%PSQL_CMD%" -h "%NEW_PRIMARY_HOST%" -p "%NEW_PRIMARY_PORT%" -U "%REPL_USER%" -d postgres -Atqc "WITH existing AS (SELECT slot_name FROM pg_replication_slots WHERE slot_name = '%SLOT_NAME%' AND slot_type = 'physical'), created AS (SELECT slot_name FROM pg_create_physical_replication_slot('%SLOT_NAME%') WHERE NOT EXISTS (SELECT 1 FROM existing)) SELECT CASE WHEN EXISTS (SELECT 1 FROM existing) THEN 'exists' WHEN EXISTS (SELECT 1 FROM created) THEN 'created' ELSE 'missing' END" 2^>nul`) do set "SLOT_STATE=%%i"
if /i "%SLOT_STATE%"=="exists" (
  call :log replication slot ready: %SLOT_NAME% (exists)
  exit /b 0
)
if /i "%SLOT_STATE%"=="created" (
  call :log replication slot ready: %SLOT_NAME% (created)
  exit /b 0
)
call :fail failed to ensure replication slot: %SLOT_NAME%

:require
if defined %~1 exit /b 0
echo [ERROR] missing env: %~1 1>&2
exit /b 1

:log
echo [INFO] %*
exit /b 0

:fail
echo [ERROR] %* 1>&2
exit /b 1