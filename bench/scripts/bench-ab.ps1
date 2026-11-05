$ErrorActionPreference = 'Stop'
$bench = 'D:\OpenCode\bench-client.ps1'
$runs = @(
    @{ Label = 'ab-aurora-on';            Mode = 'aurora-on' },
    @{ Label = 'ab-sodium-off';           Mode = 'sodium-off' },
    @{ Label = 'ab-sodium-on';            Mode = 'sodium-on' },
    @{ Label = 'ab-sodium-lod-captured';  Mode = 'sodium-lod-captured' },
    @{ Label = 'ab-sodium-lod-worldgen';  Mode = 'sodium-lod-worldgen' },
    @{ Label = 'ab-sodium-on-control';    Mode = 'sodium-on' }
)

$results = @()
foreach ($run in $runs) {
    Write-Output "=== Ejecutando $($run.Label) ($($run.Mode)) ==="
    try {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $bench -Label $run.Label -Mode $run.Mode -Seconds 30 -Warmup 60
        $txt = "D:\OpenCode\bench-$($run.Label).txt"
        if (Test-Path $txt) {
            $results += Get-Content $txt -Raw
        } else {
            $results += "LABEL=$($run.Label)`nERROR=sin resultado"
        }
    } catch {
        Write-Output "FALLO $($run.Label): $_"
        $results += "LABEL=$($run.Label)`nERROR=$_"
    }
    Start-Sleep -Seconds 5
}

$summary = $results -join "`n`n"
$summary | Set-Content 'D:\OpenCode\bench-ab-summary.txt' -Encoding UTF8
Write-Output $summary
Write-Output 'Tanda A/B terminada.'
