# 完成 Gaifan 堆叠分支 amend + PR-6 + 可选 push/gh
param(
    [switch]$Push,
    [switch]$CreatePrs
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

function Rebase-StackFrom($baseBranch) {
    $chain = @(
        'feat/freeze-platform-commercial',
        'feat/freeze-six-products-backend',
        'feat/freeze-front-commercial',
        'feat/freeze-scripts-evidence',
        'feat/freeze-infra-glue'
    )
    $base = $baseBranch
    foreach ($b in $chain) {
        if (-not (git rev-parse --verify $b 2>$null)) { continue }
        Write-Host "[REBASE] $b onto $base"
        git checkout $b
        git rebase $base
        if ($LASTEXITCODE -ne 0) { throw "rebase failed: $b" }
        $base = $b
    }
}

Write-Host "=== PR-1 amend: intelligence cleanup ==="
git checkout feat/freeze-ai-foundation
git add -u douyin-operations-intelligence/src/ 2>$null
git add douyin-operations-intelligence/ 2>$null
$gaifanDev = @(
    'douyin-operations-app/src/main/resources/application-gaifan-dev.yml',
    'douyin-operations-app/src/main/resources/application-gaifan-e2e.yml'
)
foreach ($f in $gaifanDev) { if (Test-Path $f) { git add $f } }
if (git diff --cached --quiet) {
    Write-Host "[SKIP] PR-1 amend nothing staged"
} else {
    git commit -m "chore(gaifan): intelligence shell cleanup + gaifan dev profiles"
    Write-Host "[OK] PR-1 amend committed"
}

Rebase-StackFrom 'feat/freeze-ai-foundation'

Write-Host "=== PR-4 amend: front package + entitlement wave2 ==="
git checkout feat/freeze-front-commercial
git add front/package.json front/package-lock.json front/vitest.config.ts 2>$null
git add front/e2e/ 2>$null
git add front/src/hooks/useGaifanEntitlementGate.ts front/src/hooks/useFilteredRoleNav.tsx 2>$null
git add front/src/pages/ 2>$null
if (git diff --cached --quiet) {
    Write-Host "[SKIP] PR-4 amend nothing staged"
} else {
    Push-Location front
    npm run type-check --silent
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "PR-4 type-check failed" }
    Pop-Location
    git commit -m "feat(gaifan): entitlement pre-check wave2 + nav productCode map"
    Write-Host "[OK] PR-4 amend committed"
}

Rebase-StackFrom 'feat/freeze-front-commercial'

Write-Host "=== PR-5 amend: CI + evidence ==="
git checkout feat/freeze-scripts-evidence
git add .github/workflows/six-products-nightly.yml 2>$null
git add reports/automation/ 2>$null
git add scripts/run-gaifan-pr-freeze.ps1 scripts/run-gaifan-stack-complete.ps1 scripts/start-gaifan-prod-stack.ps1 2>$null
if (git diff --cached --quiet) {
    Write-Host "[SKIP] PR-5 amend nothing staged"
} else {
    git commit -m "chore(gaifan): nightly CI + prod stack scripts + evidence sync"
}

Write-Host "=== PR-6: infra-glue ==="
git checkout -B feat/freeze-infra-glue feat/freeze-scripts-evidence
git reset
$pr6Paths = @(
    'pom.xml', '.env.example', '.gitignore',
    '.github/workflows/',
    'docker/', 'docker-compose.microservices.yml',
    'build-service.cmd', 'build-service.sh', 'deploy.cmd', 'deploy-all.cmd',
    'douyin-operations-app/src/main/',
    'douyin-operations-asset/', 'douyin-operations-content/',
    'douyin-operations-integration/', 'douyin-operations-mcp/',
    'docs/', 'sql/'
)
$exclude = @('.codegraph', '.reasonix', 'docs/deployment/', 'docs/analysis/six-products-post-m4-checklist.md')
foreach ($p in $pr6Paths) {
    if ($p.EndsWith('/')) {
        git add $p 2>$null
    } else {
        if (Test-Path $p) { git add $p }
    }
}
# scripts except verify (already in PR-5)
Get-ChildItem scripts -File | Where-Object { $_.Name -notlike 'verify-*' } | ForEach-Object { git add $_.FullName.Substring($root.Length+1) -ErrorAction SilentlyContinue }

$staged = @(git diff --cached --name-only)
Write-Host "  PR-6 staged: $($staged.Count)"
if ($staged.Count -gt 0) {
    Write-Host "[GATE] mvn compile..."
    mvn -pl douyin-operations-app -am compile -q
    if ($LASTEXITCODE -ne 0) { throw "PR-6 compile failed" }
    docker compose -f docker-compose.microservices.yml config | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "PR-6 compose config failed" }
    git commit -m "feat(gaifan): infra glue + dy05 microservices prod stack"
    Write-Host "[OK] PR-6 committed $($staged.Count) files"
}

# Update stack metadata
$meta = @{
    generatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
    pushed = $false
    stack = @()
}
$stack = @(
    @{ pr = "PR-1"; branch = "feat/freeze-ai-foundation"; base = "main" },
    @{ pr = "PR-2"; branch = "feat/freeze-platform-commercial"; base = "feat/freeze-ai-foundation" },
    @{ pr = "PR-3"; branch = "feat/freeze-six-products-backend"; base = "feat/freeze-platform-commercial" },
    @{ pr = "PR-4"; branch = "feat/freeze-front-commercial"; base = "feat/freeze-six-products-backend" },
    @{ pr = "PR-5"; branch = "feat/freeze-scripts-evidence"; base = "feat/freeze-front-commercial" },
    @{ pr = "PR-6"; branch = "feat/freeze-infra-glue"; base = "feat/freeze-scripts-evidence" }
)
foreach ($s in $stack) {
    git checkout $s.branch 2>$null
    $sha = git rev-parse --short HEAD
    $meta.stack += @{
        pr = $s.pr; branch = $s.branch; base = $s.base; commit = $sha
        compareUrl = "https://github.com/zhang-310/dy05/compare/$($s.base)...$($s.branch)"
    }
}
git checkout feat/freeze-infra-glue
$meta | ConvertTo-Json -Depth 6 | Set-Content reports/automation/pr-freeze-stacked-branches.json -Encoding UTF8
git add reports/automation/pr-freeze-stacked-branches.json
git commit -m "docs(gaifan): update stacked PR metadata" 2>$null

if ($Push) {
    foreach ($s in $stack) {
        git push -u origin $s.branch --force
    }
    git push -u origin feat/freeze-infra-glue --force
    Write-Host "[OK] all branches pushed"
}

if ($CreatePrs) {
    Remove-Item Env:GITHUB_TOKEN -ErrorAction SilentlyContinue
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $gh) { Write-Warning "gh not found; create PRs manually via compare URLs" }
    else {
        foreach ($s in $stack) {
            $title = git log -1 --format=%s $($s.branch)
            gh pr create --base $s.base --head $s.branch --title $title --body "Stacked $($s.pr). See docs/deployment/gaifan-pr-freeze-plan.md." 2>$null
        }
    }
}

Write-Host "[DONE] stack complete on feat/freeze-infra-glue"
git status -sb | Select-Object -First 5
$remaining = (git status --porcelain | Measure-Object).Count
Write-Host "Remaining dirty files: $remaining"
