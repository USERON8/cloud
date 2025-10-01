# ============================================
# Microservice Code Cleanup Automation Script
# Version: 1.0
# Author: what's up
# Date: 2025-10-01
# ============================================

# Set error handling
$ErrorActionPreference = "Stop"

# Define color output functions
function Write-Success { 
    Write-Host $args -ForegroundColor Green 
}

function Write-Warn { 
    Write-Host $args -ForegroundColor Yellow 
}

function Write-Err { 
    Write-Host $args -ForegroundColor Red 
}

function Write-Info { 
    Write-Host $args -ForegroundColor Cyan 
}

# Get project root directory
$ProjectRoot = "D:\Download\Code\sofware\cloud"
Set-Location $ProjectRoot

Write-Info "============================================"
Write-Info "  Microservice Code Cleanup Script"
Write-Info "============================================"
Write-Host ""

# Create backup records
$BackupLogFile = "$ProjectRoot\cleanup-backup-log.txt"
$CleanupReport = "$ProjectRoot\cleanup-report.txt"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Initialize report
@"
============================================
Code Cleanup Execution Report
Execution Time: $timestamp
============================================

"@ | Out-File $CleanupReport -Encoding UTF8

# Statistics variables
$FilesDeleted = 0
$FilesRenamed = 0
$DirectoriesDeleted = 0
$Errors = @()

# ============================================
# Stage 1: Check Git Status
# ============================================
Write-Info "`n[Stage 1/4] Checking Git status..."

try {
    $gitStatus = git status --porcelain 2>&1
    if ($LASTEXITCODE -eq 0) {
        if ($gitStatus) {
            Write-Warn "[WARNING] Uncommitted changes detected!"
            Write-Warn "It is recommended to commit or stash current changes first."
            $continue = Read-Host "Continue with cleanup? (y/n)"
            if ($continue -ne "y" -and $continue -ne "Y") {
                Write-Warn "Cleanup cancelled."
                exit 0
            }
        } else {
            Write-Success "[OK] Git working directory is clean"
        }
        
        # Create cleanup branch
        Write-Info "Creating cleanup branch: feature/code-cleanup"
        git checkout -b feature/code-cleanup 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Warn "[WARNING] Branch already exists or creation failed, continuing with current branch"
        } else {
            Write-Success "[OK] Cleanup branch created successfully"
        }
    } else {
        Write-Warn "[WARNING] Git repository not detected, skipping branch creation"
    }
} catch {
    Write-Warn "[WARNING] Git check failed: $_"
}

# ============================================
# Stage 2: Delete Redundant Files
# ============================================
Write-Info "`n[Stage 2/4] Deleting redundant files..."

# Define files to delete
$FilesToDelete = @(
    # User service - old Service implementations
    "user-service\src\main\java\com\cloud\user\service\impl\AdminServiceImpl.java",
    "user-service\src\main\java\com\cloud\user\service\MerchantService.java",
    "user-service\src\main\java\com\cloud\user\service\impl\MerchantServiceImpl.java",
    
    # User service - old Controllers
    "user-service\src\main\java\com\cloud\user\controller\merchant\MerchantManageController.java",
    "user-service\src\main\java\com\cloud\user\controller\merchant\MerchantQueryController.java",
    
    # Order service - SimpleOrderService
    "order-service\src\main\java\com\cloud\order\service\SimpleOrderService.java",
    "order-service\src\main\java\com\cloud\order\service\impl\SimpleOrderServiceImpl.java"
)

# Define directories to delete
$DirectoriesToDelete = @(
    # User service - admin directory
    "user-service\src\main\java\com\cloud\user\controller\admin",
    
    # Product service - backup directory
    "product-service\backup"
)

# Delete files
foreach ($file in $FilesToDelete) {
    $fullPath = Join-Path $ProjectRoot $file
    if (Test-Path $fullPath) {
        try {
            # Log to backup file
            "DELETED FILE: $file at $timestamp" | Out-File $BackupLogFile -Append -Encoding UTF8
            
            Remove-Item $fullPath -Force
            Write-Success "  [OK] Deleted file: $file"
            "[OK] Deleted file: $file" | Out-File $CleanupReport -Append -Encoding UTF8
            $FilesDeleted++
        } catch {
            Write-Err "  [ERROR] Failed to delete: $file - $_"
            "[ERROR] Failed to delete: $file - $_" | Out-File $CleanupReport -Append -Encoding UTF8
            $Errors += "Failed to delete file: $file"
        }
    } else {
        Write-Warn "  [SKIP] File not found: $file"
        "[SKIP] File not found: $file" | Out-File $CleanupReport -Append -Encoding UTF8
    }
}

# Delete directories
foreach ($dir in $DirectoriesToDelete) {
    $fullPath = Join-Path $ProjectRoot $dir
    if (Test-Path $fullPath) {
        try {
            # Log to backup file
            "DELETED DIRECTORY: $dir at $timestamp" | Out-File $BackupLogFile -Append -Encoding UTF8
            
            Remove-Item $fullPath -Recurse -Force
            Write-Success "  [OK] Deleted directory: $dir"
            "[OK] Deleted directory: $dir" | Out-File $CleanupReport -Append -Encoding UTF8
            $DirectoriesDeleted++
        } catch {
            Write-Err "  [ERROR] Failed to delete: $dir - $_"
            "[ERROR] Failed to delete: $dir - $_" | Out-File $CleanupReport -Append -Encoding UTF8
            $Errors += "Failed to delete directory: $dir"
        }
    } else {
        Write-Warn "  [SKIP] Directory not found: $dir"
        "[SKIP] Directory not found: $dir" | Out-File $CleanupReport -Append -Encoding UTF8
    }
}

# ============================================
# Stage 3: Rename Standardized Files
# ============================================
Write-Info "`n[Stage 3/4] Renaming standardized files..."

# Define rename mappings
$RenameMapping = @(
    @{
        Old = "user-service\src\main\java\com\cloud\user\service\impl\AdminServiceImplNew.java"
        New = "user-service\src\main\java\com\cloud\user\service\impl\AdminServiceImpl.java"
        Description = "AdminService Implementation"
    },
    @{
        Old = "user-service\src\main\java\com\cloud\user\service\MerchantServiceStandard.java"
        New = "user-service\src\main\java\com\cloud\user\service\MerchantService.java"
        Description = "MerchantService Interface"
    },
    @{
        Old = "user-service\src\main\java\com\cloud\user\service\impl\MerchantServiceImplStandard.java"
        New = "user-service\src\main\java\com\cloud\user\service\impl\MerchantServiceImpl.java"
        Description = "MerchantService Implementation"
    }
)

foreach ($rename in $RenameMapping) {
    $oldPath = Join-Path $ProjectRoot $rename.Old
    $newPath = Join-Path $ProjectRoot $rename.New
    
    if (Test-Path $oldPath) {
        try {
            # Check if target file already exists
            if (Test-Path $newPath) {
                Write-Warn "  [SKIP] Target file already exists: $($rename.Description)"
                "[SKIP] Target file already exists: $($rename.Description)" | Out-File $CleanupReport -Append -Encoding UTF8
                continue
            }
            
            # Log to backup file
            "RENAMED: $($rename.Old) -> $($rename.New) at $timestamp" | Out-File $BackupLogFile -Append -Encoding UTF8
            
            Move-Item $oldPath $newPath -Force
            Write-Success "  [OK] Renamed: $($rename.Description)"
            "[OK] Renamed: $($rename.Description)" | Out-File $CleanupReport -Append -Encoding UTF8
            $FilesRenamed++
        } catch {
            Write-Err "  [ERROR] Failed to rename: $($rename.Description) - $_"
            "[ERROR] Failed to rename: $($rename.Description) - $_" | Out-File $CleanupReport -Append -Encoding UTF8
            $Errors += "Failed to rename: $($rename.Description)"
        }
    } else {
        Write-Warn "  [SKIP] Source file not found: $($rename.Description)"
        "[SKIP] Source file not found: $($rename.Description)" | Out-File $CleanupReport -Append -Encoding UTF8
    }
}

# ============================================
# Stage 4: Generate Cleanup Report
# ============================================
Write-Info "`n[Stage 4/4] Generating cleanup report..."

# Summary statistics
$reportSummary = @"

============================================
Cleanup Statistics
============================================
Files Deleted: $FilesDeleted
Directories Deleted: $DirectoriesDeleted
Files Renamed: $FilesRenamed
Errors: $($Errors.Count)

"@

$reportSummary | Out-File $CleanupReport -Append -Encoding UTF8

if ($Errors.Count -gt 0) {
    "`nError Details:" | Out-File $CleanupReport -Append -Encoding UTF8
    foreach ($error in $Errors) {
        "  - $error" | Out-File $CleanupReport -Append -Encoding UTF8
    }
}

# Console output
Write-Host ""
Write-Info "============================================"
Write-Info "  Cleanup Completed!"
Write-Info "============================================"
Write-Host ""
Write-Success "[OK] Files Deleted: $FilesDeleted"
Write-Success "[OK] Directories Deleted: $DirectoriesDeleted"
Write-Success "[OK] Files Renamed: $FilesRenamed"

if ($Errors.Count -gt 0) {
    Write-Warn "`n[WARNING] Found $($Errors.Count) errors, please check the report file"
}

Write-Host ""
Write-Info "Report saved to: $CleanupReport"
Write-Info "Backup log saved to: $BackupLogFile"

# ============================================
# Git Commit (Optional)
# ============================================
Write-Host ""
$commitChanges = Read-Host "Commit changes to Git? (y/n)"

if ($commitChanges -eq "y" -or $commitChanges -eq "Y") {
    try {
        Write-Info "`nCommitting changes to Git..."
        
        git add -A
        $commitMessage = @"
chore: Clean up redundant code and standardize Service layer

- Deleted $FilesDeleted redundant files
- Deleted $DirectoriesDeleted redundant directories
- Renamed $FilesRenamed standardized files

See cleanup-report.txt for details
"@
        
        git commit -m $commitMessage
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "[OK] Git commit successful"
            Write-Info "`nYou can push changes with:"
            Write-Info "  git push origin feature/code-cleanup"
        } else {
            Write-Warn "[WARNING] Git commit failed"
        }
    } catch {
        Write-Err "[ERROR] Git commit failed: $_"
    }
}

# ============================================
# Next Steps
# ============================================
Write-Host ""
Write-Info "============================================"
Write-Info "  Next Steps"
Write-Info "============================================"
Write-Host ""
Write-Info "1. Run tests to ensure functionality:"
Write-Info "   mvn clean test"
Write-Host ""
Write-Info "2. Update code that references these files (if any)"
Write-Host ""
Write-Info "3. To rollback, use:"
Write-Info "   git reset --hard HEAD~1"
Write-Host ""
Write-Success "[OK] Cleanup script completed!"
Write-Host ""

