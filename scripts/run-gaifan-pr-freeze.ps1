# Gaifan 5 PR 冻结执行器 — 见 docs/deployment/gaifan-pr-freeze-plan.md
# 用法：pwsh scripts/run-gaifan-pr-freeze.ps1 -Execute   # 需显式 -Execute 才会 commit
param(
    [switch]$Execute,
    [switch]$Push,
    [string]$BaseBranch = "main",
    [int]$StartFrom = 1
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$prs = @(
    @{
        Name = "PR-1"
        Branch = "feat/freeze-ai-foundation"
        Paths = @(
            "douyin-operations-ai/",
            "douyin-operations-intelligence/",
            "douyin-operations-contract/",
            "douyin-operations-common/",
            "douyin-operations-app/pom.xml",
            "douyin-operations-app/src/test/",
            "douyin-operations-app/src/main/resources/db/migration/V209",
            "douyin-operations-app/src/main/resources/db/migration/V21",
            "douyin-operations-app/src/main/resources/db/migration/V22",
            "douyin-operations-app/src/main/resources/application-prod.yml",
            "douyin-operations-app/src/main/resources/application-gaifan-staging.yml",
            "douyin-operations-app/src/main/resources/application-gaifan-dev.yml",
            "douyin-operations-app/src/main/resources/application-gaifan-e2e.yml",
            "scripts/seed-gaifan-kb-demo.sql",
            "scripts/seed-gaifan-jwt-tenant.sql",
            "scripts/seed-gaifan-payment-verify.sql",
            "scripts/apply-gaifan-product-flyway.ps1"
        )
        Gate = { pwsh scripts/run-ai-foundation-verify.ps1 -ErrorAction Stop }
        Message = "feat(gaifan): AI foundation + Flyway V209-V227 + KB pgvector"
    },
    @{
        Name = "PR-2"
        Branch = "feat/freeze-platform-commercial"
        Paths = @("douyin-operations-platform/", "douyin-operations-payment/")
        Gate = {
            mvn -pl douyin-operations-platform "-Dtest=ProductIntegrationServiceTest" -DfailIfNoTests=false test -q
            if ($LASTEXITCODE -ne 0) { throw "PR-2 platform gate failed" }
            mvn -pl douyin-operations-app "-Dtest=PaymentCreditGrantAdapterPgTest" -DfailIfNoTests=false test -q
            if ($LASTEXITCODE -ne 0) { throw "PR-2 payment-pg gate failed" }
        }
        Message = "feat(gaifan): platform commercial + payment credit grant"
    },
    @{
        Name = "PR-3"
        Branch = "feat/freeze-six-products-backend"
        Paths = @(
            "douyin-operations-shortvideo/",
            "douyin-operations-drama/",
            "douyin-operations-digital-human/",
            "douyin-operations-photo-avatar/",
            "douyin-operations-douyin/",
            "douyin-operations-live/"
        )
        Gate = {
            mvn -pl douyin-operations-shortvideo,douyin-operations-app "-Dtest=BenchmarkAnalysisChargeTest,DouyinOAuthCallbackChargeTest" -DfailIfNoTests=false test -q
            if ($LASTEXITCODE -ne 0) { throw "PR-3 gate failed" }
        }
        Message = "feat(gaifan): six products backend + feature verify hooks"
    },
    @{
        Name = "PR-4"
        Branch = "feat/freeze-front-commercial"
        Paths = @("front/src/", "front/package.json", "front/package-lock.json", "front/vitest.config.ts", "front/e2e/")
        Gate = {
            Push-Location front
            npm run type-check --silent
            if ($LASTEXITCODE -ne 0) { Pop-Location; throw "PR-4 type-check failed" }
            Pop-Location
        }
        Message = "feat(gaifan): front commercial nav + entitlement pre-check"
    },
    @{
        Name = "PR-5"
        Branch = "feat/freeze-scripts-evidence"
        Paths = @(
            "scripts/verify-",
            "scripts/run-six-products-ci.ps1",
            "scripts/sign-gaifan-prod-smoke.ps1",
            "scripts/run-gaifan-pr-freeze.ps1",
            "scripts/run-gaifan-stack-complete.ps1",
            "scripts/start-gaifan-prod-stack.ps1",
            ".github/workflows/six-products-nightly.yml",
            "reports/automation/",
            "docs/deployment/",
            "docs/analysis/six-products-post-m4-checklist.md"
        )
        Gate = {
            $env:GAIFAN_MATRIX_SKIP_M4 = "true"
            if ($env:GAIFAN_VERIFY_BASE_URL) {
                pwsh scripts/run-six-products-ci.ps1 -FullSellable -BaseUrl $env:GAIFAN_VERIFY_BASE_URL
            } else {
                pwsh scripts/run-six-products-ci.ps1 -SkipHttpVerify
            }
            if ($LASTEXITCODE -ne 0) { throw "PR-5 gate failed" }
        }
        Message = "chore(gaifan): verify scripts + staging evidence + release docs"
    },
    @{
        Name = "PR-6"
        Branch = "feat/freeze-infra-glue"
        Paths = @(
            "pom.xml", ".env.example", ".gitignore",
            ".github/workflows/",
            "docker/", "docker-compose.microservices.yml",
            "build-service.cmd", "build-service.sh", "deploy.cmd", "deploy-all.cmd",
            "douyin-operations-app/src/main/",
            "douyin-operations-asset/", "douyin-operations-content/",
            "douyin-operations-integration/", "douyin-operations-mcp/"
        )
        Gate = {
            mvn -pl douyin-operations-app -am compile -q
            if ($LASTEXITCODE -ne 0) { throw "PR-6 compile failed" }
            docker compose -f docker-compose.microservices.yml config | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "PR-6 compose config failed" }
        }
        Message = "feat(gaifan): infra glue + dy05 microservices prod stack"
    }
)

function Add-PathsForPr($paths) {
    $files = @()
    foreach ($p in $paths) {
        if ($p.EndsWith("/")) {
            $files += git ls-files $p 2>$null
            $files += git ls-files --others --exclude-standard $p 2>$null
        } elseif ($p -match "V209|V21|V22") {
            $files += git ls-files "douyin-operations-app/src/main/resources/db/migration/" |
                Where-Object { $_ -match "V(209|21[0-9]|22[0-7])" }
            $files += git ls-files --others --exclude-standard "douyin-operations-app/src/main/resources/db/migration/" |
                Where-Object { $_ -match "V(209|21[0-9]|22[0-7])" }
        } elseif ($p -like "scripts/verify-*") {
            $files += Get-ChildItem -Path "scripts" -Filter "verify-*" -File | ForEach-Object { $_.FullName.Substring($root.Length + 1) -replace '\\','/' }
        } else {
            $files += git ls-files $p 2>$null
            $files += git ls-files --others --exclude-standard $p 2>$null
        }
    }
    $files | Where-Object { $_ } | Select-Object -Unique
}

Write-Host "=== Gaifan PR freeze plan (Execute=$Execute Push=$Push StartFrom=$StartFrom) ==="
$stackBase = $BaseBranch
$prIndex = 0
foreach ($pr in $prs) {
    $prIndex++
    if ($prIndex -lt $StartFrom) {
        $stackBase = $pr.Branch
        Write-Host "`n--- $($pr.Name) $($pr.Branch) [SKIP StartFrom] ---"
        continue
    }
    Write-Host "`n--- $($pr.Name) $($pr.Branch) base=$stackBase ---"
    $files = Add-PathsForPr $pr.Paths
    Write-Host "  files: $($files.Count)"
    if (-not $Execute) {
        Write-Host "  [DRY-RUN] gate + commit skipped (pass -Execute to run)"
        continue
    }

    $branchTip = git rev-parse --verify $pr.Branch 2>$null
    if ($branchTip -and (git merge-base --is-ancestor $stackBase $branchTip)) {
        git checkout $pr.Branch
        $msg = git log -1 --format=%s $branchTip
        if ($msg -eq $pr.Message) {
            Write-Host "  [SKIP] branch already at commit: $msg"
            $stackBase = $pr.Branch
            continue
        }
    }
    git checkout -B $pr.Branch $stackBase
    git reset
    foreach ($f in $files) {
        if (Test-Path $f) { git add $f }
    }
    $staged = @(git diff --cached --name-only)
    if ($staged.Count -eq 0) {
        Write-Host "  [SKIP] no staged files"
        $stackBase = $pr.Branch
        continue
    }

    Write-Host "  [GATE] running..."
    & $pr.Gate

    git commit -m $pr.Message
    Write-Host "  [OK] committed $($staged.Count) files"

    if ($Push) {
        git push -u origin $pr.Branch --force
        $prBody = @"
Automated freeze $($pr.Name). See docs/deployment/gaifan-pr-freeze-plan.md.

Stack base: $stackBase
"@
        $gh = Get-Command gh -ErrorAction SilentlyContinue
        if (-not $gh) {
            Write-Warning "  gh CLI not found; push OK but PR not created. Install GitHub CLI or run: gh pr create --base $stackBase --head $($pr.Branch) --title '$($pr.Message)'"
        } else {
            gh pr create --base $stackBase --head $pr.Branch --title $pr.Message --body $prBody
            Write-Host "  [OK] pushed + PR created (base=$stackBase)"
        }
    }

    $stackBase = $pr.Branch
}

if (-not $Execute) {
    Write-Host "`nDry-run complete. Re-run with -Execute to create branches/commits; add -Push for remote PRs."
}
