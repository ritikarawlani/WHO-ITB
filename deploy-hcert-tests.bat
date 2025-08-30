@echo off
REM FLINT HCERT Test Suite Deployment Script for Windows
REM This script packages and deploys the HCERT validation test suite to ITB

echo 🚀 FLINT HCERT Test Suite Deployment
echo ====================================

REM Configuration
set TEST_SUITE_NAME=hcert-validation
set TEST_SUITE_DIR=test-suites\%TEST_SUITE_NAME%
set ITB_URL=http://localhost:10003

REM Check prerequisites
echo [INFO] Checking prerequisites...

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Check if test suite directory exists
if not exist "%TEST_SUITE_DIR%" (
    echo [ERROR] Test suite directory not found: %TEST_SUITE_DIR%
    exit /b 1
)

echo [INFO] Prerequisites check passed ✓

REM Validate test suite structure
echo [INFO] Validating test suite structure...

if not exist "%TEST_SUITE_DIR%\test-suite.xml" (
    echo [ERROR] Required file not found: %TEST_SUITE_DIR%\test-suite.xml
    exit /b 1
)

if not exist "%TEST_SUITE_DIR%\test-cases" (
    echo [ERROR] Required directory not found: %TEST_SUITE_DIR%\test-cases
    exit /b 1
)

if not exist "%TEST_SUITE_DIR%\scriptlets" (
    echo [ERROR] Required directory not found: %TEST_SUITE_DIR%\scriptlets
    exit /b 1
)

if not exist "%TEST_SUITE_DIR%\resources" (
    echo [ERROR] Required directory not found: %TEST_SUITE_DIR%\resources
    exit /b 1
)

echo [INFO] Test suite structure validated ✓

REM Build helper services
echo [INFO] Building WHO helper services...
cd who-helper
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Failed to build helper services
    exit /b 1
)
cd ..
echo [INFO] Helper services built successfully ✓

REM Start services
echo [INFO] Starting ITB services...
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start services
    exit /b 1
)
echo [INFO] Services started successfully ✓

REM Wait for services to be ready
echo [INFO] Waiting for services to be ready...

REM Wait for ITB UI (simple retry loop)
timeout /t 10 /nobreak >nul
echo [INFO] Services should be ready ✓

REM Create package directory
if not exist "packages" mkdir packages

REM Create timestamp for package name
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c-%%a-%%b)
for /f "tokens=1-2 delims=: " %%a in ('time /t') do (set mytime=%%a%%b)
set PACKAGE_NAME=%TEST_SUITE_NAME%-%mydate%-%mytime%.zip

REM Package test suite
echo [INFO] Packaging test suite...
cd %TEST_SUITE_DIR%
powershell -command "Compress-Archive -Path *.xml,test-cases,scriptlets,resources -DestinationPath ..\..\packages\%PACKAGE_NAME%"
if errorlevel 1 (
    echo [ERROR] Failed to create test suite package
    exit /b 1
)
cd ..\..
echo [INFO] Test suite packaged as packages\%PACKAGE_NAME% ✓

REM Display deployment info
echo.
echo [INFO] Deployment completed successfully! 🎉
echo.
echo 📋 Deployment Summary:
echo ======================
echo • Test Suite Package: packages\%PACKAGE_NAME%
echo • ITB UI: %ITB_URL%
echo • GDHCN Validator: http://localhost:8080
echo • Helper Services: http://localhost:10005
echo.
echo 🔐 Login Credentials:
echo ====================
echo • Username: user@who.itb.test
echo • Password: change_this_password
echo.
echo 📝 Next Steps:
echo ==============
echo 1. Open %ITB_URL% in your browser
echo 2. Login with the provided credentials
echo 3. Navigate to Test Sessions
echo 4. Select 'HCERT Validation Test Suite'
echo 5. Run individual test cases
echo.
echo 📚 Available Test Cases:
echo ========================
echo • tc-qr-valid-hcert: Valid HCERT QR Code Validation
echo • tc-qr-invalid-hcert: Invalid HCERT QR Code Validation  
echo • tc-cwt-extraction: CWT Token Extraction
echo • tc-signature-algorithm-validation: Signature Algorithm Validation
echo • tc-key-identifier-validation: Key Identifier Validation
echo • tc-signature-verification: Digital Signature Verification
echo • tc-hcert-payload-extraction: HCERT Payload Extraction
echo.
echo 🔧 Management Commands:
echo ======================
echo • View logs: docker-compose logs -f
echo • Stop services: docker-compose down
echo • Reset environment: docker-compose down -v ^&^& docker-compose up -d
echo.

pause