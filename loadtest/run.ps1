param([int]$Vus = 50, [string]$Duration = "30s", [string]$Warmup = "20s", [int]$Runs = 3)

$root = Split-Path -Parent $PSScriptRoot
$results = Join-Path $PSScriptRoot "results"
New-Item -ItemType Directory -Force $results | Out-Null
Set-Location $root

function K6([double]$writeRatio, [string]$duration) {
    docker run --rm --network game-leaderboard_default -v "${PSScriptRoot}:/scripts" grafana/k6:1.3.0 `
        run --quiet -e VUS=$Vus -e DURATION=$duration -e WRITE_RATIO=$writeRatio /scripts/leaderboard.js 2>&1
}

foreach ($cache in @("true", "false")) {
    $env:CACHE_ENABLED = $cache
    $env:RATE_LIMIT_ENABLED = "false"
    docker compose up -d --force-recreate --wait app 2>&1 | Out-Null
    docker compose exec -T redis redis-cli FLUSHALL | Out-Null
    $label = if ($cache -eq "true") { "cache-on" } else { "cache-off" }

    foreach ($mix in @(@{ name = "reads"; ratio = 0 }, @{ name = "mixed"; ratio = 0.1 })) {
        K6 $mix.ratio $Warmup | Out-Null
        for ($i = 1; $i -le $Runs; $i++) {
            $file = Join-Path $results "$label-$($mix.name)-run$i.txt"
            K6 $mix.ratio $Duration | Out-File -Encoding utf8 $file
            Write-Host "== $label $($mix.name) run $i"
            Get-Content $file | Where-Object { $_ -match "RESULT|p50=" }
        }
    }
}
Remove-Item Env:CACHE_ENABLED, Env:RATE_LIMIT_ENABLED
docker compose up -d --force-recreate --wait app 2>&1 | Out-Null
