#Requires -Version 5.1
<#
.SYNOPSIS
    Cut a ShrinkGrow release: format, commit, tag, build, publish.

.DESCRIPTION
    For day-to-day iteration just run  .\gradlew build  directly. Those jars are
    named  1.0.1-2-gabc1234-dirty , which is a deliberate signal that they are
    not releases. Use this script only when you want a version your friends can
    download.

    Order matters here:
      spotlessApply runs BEFORE the commit, because it rewrites source files -
      formatting after committing would leave the tree dirty and the jar would
      be named "-dirty" despite being a tagged release.
      The tag is created BEFORE the build, because the jar name comes from
      git describe.

.EXAMPLE
    .\release.ps1 1.0.2 "Fix conduit right-click while shrunk"

.EXAMPLE
    .\release.ps1 1.0.2 "Fix conduit right-click" -Deploy
    Also copies the jar into the local Minecraft instances listed below.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version,

    [Parameter(Position = 1)]
    [string]$Message,

    # Copy the finished jar into the instances in $InstanceModFolders.
    [switch]$Deploy,

    # Create the GitHub release as a draft so you can edit notes before it goes live.
    [switch]$Draft,

    # Build and tag locally, but do not push or publish.
    [switch]$NoPublish
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Edit these if your instance paths change.
# ---------------------------------------------------------------------------
$InstanceModFolders = @(
    "C:\Users\Will Keegan\AppData\Roaming\PrismLauncher\instances\Greg 2.9 RC1\.minecraft\mods"
)
# ---------------------------------------------------------------------------

function Fail([string]$Text) {
    Write-Host ""
    Write-Host "ERROR: $Text" -ForegroundColor Red
    exit 1
}
function Step([string]$Text) {
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}
function Ok([string]$Text) {
    Write-Host "    $Text" -ForegroundColor Green
}

Set-Location $PSScriptRoot

if (-not $Message) { $Message = "ShrinkGrow $Version" }

# --- sanity checks -----------------------------------------------------------
if (-not (Test-Path ".\gradlew.bat")) { Fail "No gradlew.bat here. Run this from the repo root." }

git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) { Fail "This folder is not a git repository." }

if (git tag --list $Version) {
    Fail "Tag $Version already exists. Pick a new version number (released versions are never reused)."
}

# --- 1. format, before committing -------------------------------------------
Step "Formatting (spotlessApply)"
.\gradlew.bat spotlessApply
if ($LASTEXITCODE -ne 0) { Fail "spotlessApply failed." }

# --- 2. commit ---------------------------------------------------------------
Step "Committing"
git add -A
if (git status --porcelain) {
    git commit -m $Message
    if ($LASTEXITCODE -ne 0) { Fail "Commit failed." }
    Ok "Committed: $Message"
}
else {
    Ok "Nothing changed - tagging the existing commit."
}

# --- 3. tag, before building -------------------------------------------------
Step "Tagging $Version"
git tag -a $Version -m $Message
if ($LASTEXITCODE -ne 0) { Fail "Tagging failed." }

# From here on, any failure should remove the tag so you can retry cleanly.
function RollbackTag {
    Write-Host "    Removing tag $Version so you can retry." -ForegroundColor Yellow
    git tag -d $Version | Out-Null
}

# --- 4. build ----------------------------------------------------------------
Step "Building"
.\gradlew.bat build
if ($LASTEXITCODE -ne 0) { RollbackTag; Fail "Build failed." }

# --- 5. verify the jar is named exactly what we expect ------------------------
$jar = "build\libs\shrinkgrow-$Version.jar"
if (-not (Test-Path $jar)) {
    $produced = Get-ChildItem "build\libs\*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(dev|sources)\.jar$' } |
        Select-Object -First 1
    RollbackTag
    if ($produced) {
        Fail "Expected shrinkgrow-$Version.jar but got $($produced.Name). Something is off with git describe."
    }
    Fail "No jar was produced."
}
$jarFull = (Resolve-Path $jar).Path
Ok "Built $jar"

# --- 6. publish --------------------------------------------------------------
if ($NoPublish) {
    Step "Skipping publish (-NoPublish)"
    Ok "Jar: $jarFull"
}
elseif (-not (git remote)) {
    Step "No git remote configured - skipping push and GitHub release"
    Ok "Jar: $jarFull"
}
else {
    Step "Pushing to GitHub"
    $branch = git rev-parse --abbrev-ref HEAD
    git push origin $branch
    if ($LASTEXITCODE -ne 0) { Fail "Push failed. The tag exists locally; fix the remote and re-push." }
    git push origin $Version
    if ($LASTEXITCODE -ne 0) { Fail "Pushing the tag failed." }
    Ok "Pushed $branch and tag $Version"

    Step "Creating GitHub release"
    $ghArgs = @(
        'release', 'create', $Version, $jarFull,
        '--title', "ShrinkGrow $Version",
        '--notes', $Message
    )
    if ($Draft) { $ghArgs += '--draft' }

    gh @ghArgs
    if ($LASTEXITCODE -ne 0) { Fail "gh release create failed. The tag is pushed; you can retry just this step." }

    $url = gh release view $Version --json url --jq .url
    Ok "Released: $url"
    Write-Host ""
    Write-Host "    Send your friends this link - the jar is attached to it." -ForegroundColor Green
}

# --- 7. optional local deploy ------------------------------------------------
if ($Deploy) {
    Step "Deploying locally"
    foreach ($folder in $InstanceModFolders) {
        if (-not (Test-Path $folder)) {
            Write-Host "    Skipped (not found): $folder" -ForegroundColor Yellow
            continue
        }
        Get-ChildItem (Join-Path $folder "shrinkgrow-*.jar") -ErrorAction SilentlyContinue |
            ForEach-Object {
                Remove-Item $_.FullName -Force
                Write-Host "    Removed old $($_.Name)"
            }
        Copy-Item $jarFull $folder
        Ok "Installed into $folder"
    }
    Write-Host ""
    Write-Host "    Remember the server needs this same jar too." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done - ShrinkGrow $Version" -ForegroundColor Green
