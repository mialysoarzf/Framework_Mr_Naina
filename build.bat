@echo off
echo ================================
echo    BUILD ET DEPLOY SCRIPT XAMPP
echo ================================

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo.
echo 1. Compilation du Framework...
cd Framework
call mvn clean compile package
if %ERRORLEVEL% neq 0 (
    echo ERREUR: La compilation du Framework a echoue!
    pause
    exit /b 1
)

echo.
echo 2. Copie du JAR vers le projet Test...
cd ..\Test
if not exist "lib" mkdir lib
copy "..\Framework\target\mr-naina-framework-1.0.0.jar" "lib\"

echo.
echo 3. Compilation du projet Test...
call mvn clean compile package
if %ERRORLEVEL% neq 0 (
    echo ERREUR: La compilation du projet Test a echoue!
    pause
    exit /b 1
)

echo.
echo 4. Verification du contenu du WAR...
echo Contenu de WEB-INF/classes:
jar -tf target\test-project-1.0.0.war | findstr "WEB-INF/classes"
echo.
echo Contenu de WEB-INF/lib:
jar -tf target\test-project-1.0.0.war | findstr "WEB-INF/lib"

echo.
echo 5. Deploiement sur XAMPP Tomcat...
set XAMPP_WEBAPPS=C:\apache-tomcat-10.1.28\webapps

if not exist "%XAMPP_WEBAPPS%" (
    echo ERREUR: Dossier XAMPP Tomcat webapps introuvable: %XAMPP_WEBAPPS%
    pause
    exit /b 1
)

echo Arret de Tomcat...
taskkill /f /im java.exe 2>nul

echo Suppression de l'ancienne application...
if exist "%XAMPP_WEBAPPS%\test-project-1.0.0" rmdir /s /q "%XAMPP_WEBAPPS%\test-project-1.0.0"
if exist "%XAMPP_WEBAPPS%\test-project-1.0.0.war" del "%XAMPP_WEBAPPS%\test-project-1.0.0.war"

echo Copie du nouveau WAR...
copy "target\test-project-1.0.0.war" "%XAMPP_WEBAPPS%\"

echo Demarrage de Tomcat...
start "Tomcat" "C:\apache-tomcat-10.1.28\bin\startup.bat"

echo Attente du demarrage (15 secondes)...
timeout 15 >nul

echo.
echo 6. Ouverture de l'application...
start http://localhost:8080/test-project-1.0.0/

echo.
echo ================================
echo     DEPLOIEMENT TERMINE!
echo ================================
echo Application sur: http://localhost:8080/test-project-1.0.0/
echo.
echo Consultez les logs Tomcat dans: C:\apache-tomcat-10.1.28\logs\catalina.out
pause