#!/bin/bash

# FLINT HCERT Test Suite Deployment Script
# This script packages and deploys the HCERT validation test suite to ITB

set -e

echo "🚀 FLINT HCERT Test Suite Deployment"
echo "===================================="

# Configuration
TEST_SUITE_NAME="hcert-validation"
TEST_SUITE_DIR="test-suites/${TEST_SUITE_NAME}"
PACKAGE_NAME="${TEST_SUITE_NAME}-$(date +%Y%m%d-%H%M%S).zip"
ITB_URL="http://localhost:10003"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    
    # Check if test suite directory exists
    if [ ! -d "$TEST_SUITE_DIR" ]; then
        log_error "Test suite directory not found: $TEST_SUITE_DIR"
        exit 1
    fi
    
    # Check if zip is available
    if ! command -v zip &> /dev/null; then
        log_error "zip command not found. Please install zip utility."
        exit 1
    fi
    
    log_info "Prerequisites check passed ✓"
}

validate_test_suite() {
    log_info "Validating test suite structure..."
    
    # Check for required files
    required_files=(
        "$TEST_SUITE_DIR/test-suite.xml"
        "$TEST_SUITE_DIR/test-cases"
        "$TEST_SUITE_DIR/scriptlets"
        "$TEST_SUITE_DIR/resources"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -e "$file" ]; then
            log_error "Required file/directory not found: $file"
            exit 1
        fi
    done
    
    # Count test cases
    test_case_count=$(find "$TEST_SUITE_DIR/test-cases" -name "*.xml" | wc -l)
    scriptlet_count=$(find "$TEST_SUITE_DIR/scriptlets" -name "*.xml" | wc -l)
    
    log_info "Found $test_case_count test cases and $scriptlet_count scriptlets ✓"
}

build_services() {
    log_info "Building WHO helper services..."
    
    cd who-helper
    
    # Build the helper services
    if mvn clean package -q; then
        log_info "Helper services built successfully ✓"
    else
        log_error "Failed to build helper services"
        exit 1
    fi
    
    cd ..
}

start_services() {
    log_info "Starting ITB services..."
    
    # Start services
    if docker-compose up -d; then
        log_info "Services started successfully ✓"
    else
        log_error "Failed to start services"
        exit 1
    fi
    
    # Wait for services to be healthy
    log_info "Waiting for services to be ready..."
    
    # Wait for ITB UI
    for i in {1..30}; do
        if curl -s "$ITB_URL" > /dev/null 2>&1; then
            log_info "ITB UI is ready ✓"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "ITB UI did not start within expected time"
            exit 1
        fi
        sleep 2
    done
    
    # Wait for GDHCN validator
    for i in {1..30}; do
        if curl -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
            log_info "GDHCN validator is ready ✓"
            break
        fi
        if [ $i -eq 30 ]; then
            log_warn "GDHCN validator health check failed, but continuing..."
            break
        fi
        sleep 2
    done
    
    # Wait for helper services
    for i in {1..30}; do
        if curl -s "http://localhost:10005/flint/services/process?wsdl" > /dev/null 2>&1; then
            log_info "Helper services are ready ✓"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "Helper services did not start within expected time"
            exit 1
        fi
        sleep 2
    done
}

package_test_suite() {
    log_info "Packaging test suite..."
    
    # Create package directory
    mkdir -p packages
    
    # Create zip package
    cd "$TEST_SUITE_DIR"
    if zip -r "../../packages/$PACKAGE_NAME" .; then
        log_info "Test suite packaged as packages/$PACKAGE_NAME ✓"
    else
        log_error "Failed to create test suite package"
        exit 1
    fi
    cd ../..
}

display_deployment_info() {
    log_info "Deployment completed successfully! 🎉"
    echo ""
    echo "📋 Deployment Summary:"
    echo "======================"
    echo "• Test Suite Package: packages/$PACKAGE_NAME"
    echo "• ITB UI: $ITB_URL"
    echo "• GDHCN Validator: http://localhost:8080"
    echo "• Helper Services: http://localhost:10005"
    echo ""
    echo "🔐 Login Credentials:"
    echo "===================="
    echo "• Username: user@who.itb.test"
    echo "• Password: change_this_password"
    echo ""
    echo "📝 Next Steps:"
    echo "=============="
    echo "1. Open $ITB_URL in your browser"
    echo "2. Login with the provided credentials"
    echo "3. Navigate to Test Sessions"
    echo "4. Select 'HCERT Validation Test Suite'"
    echo "5. Run individual test cases"
    echo ""
    echo "📚 Available Test Cases:"
    echo "========================"
    
    # List test cases
    find "$TEST_SUITE_DIR/test-cases" -name "*.xml" | while read -r file; do
        case_name=$(basename "$file" .xml)
        case_title=$(grep -o '<gitb:name>.*</gitb:name>' "$file" | sed 's/<[^>]*>//g' || echo "Unknown")
        echo "• $case_name: $case_title"
    done
    
    echo ""
    echo "🔧 Management Commands:"
    echo "======================"
    echo "• View logs: docker-compose logs -f"
    echo "• Stop services: docker-compose down"
    echo "• Reset environment: docker-compose down -v && docker-compose up -d"
    echo ""
}

show_help() {
    echo "FLINT HCERT Test Suite Deployment Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --skip-build        Skip building helper services"
    echo "  --skip-services     Skip starting services (assume already running)"
    echo "  --package-only      Only create test suite package"
    echo "  --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                  Full deployment (build + start + package)"
    echo "  $0 --skip-build     Deploy without rebuilding helper services"
    echo "  $0 --package-only   Only create the test suite package"
    echo ""
}

# Main execution
main() {
    local skip_build=false
    local skip_services=false
    local package_only=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-build)
                skip_build=true
                shift
                ;;
            --skip-services)
                skip_services=true
                shift
                ;;
            --package-only)
                package_only=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Execute deployment steps
    check_prerequisites
    validate_test_suite
    
    if [ "$package_only" = true ]; then
        package_test_suite
        log_info "Package created: packages/$PACKAGE_NAME"
        exit 0
    fi
    
    if [ "$skip_build" = false ]; then
        build_services
    fi
    
    if [ "$skip_services" = false ]; then
        start_services
    fi
    
    package_test_suite
    display_deployment_info
}

# Run main function with all arguments
main "$@"