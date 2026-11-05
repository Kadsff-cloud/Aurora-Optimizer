param(
    [string]$Artifacts = 'D:\Cosas\AI\opencode\projects\aurora-optimizer\artifacts',
    [string]$Root = 'D:\Cosas\AI\opencode\projects\aurora-optimizer'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$dist = Join-Path $Root 'dist'
$stage = Join-Path $Root 'build\dist-stage'

$jars = @(
    'Aurora-0.1.0.jar',
    'Aurora-0.1.0-core.jar',
    'Aurora-0.1.0-sodium.jar',
    'Aurora-0.1.0-sources.jar'
)
foreach ($jar in $jars) {
    if (-not (Test-Path (Join-Path $Artifacts $jar))) {
        throw "Falta $jar en $Artifacts. Compila los tres perfiles y copia los artefactos antes de empaquetar."
    }
}

if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
New-Item -ItemType Directory -Path $dist | Out-Null

foreach ($jar in $jars) {
    Copy-Item (Join-Path $Artifacts $jar) (Join-Path $dist $jar) -Force
}
Copy-Item (Join-Path $Root 'README.md') $dist -Force
Copy-Item (Join-Path $Root 'README.en.md') $dist -Force
Copy-Item (Join-Path $Root 'LICENSE') $dist -Force
Copy-Item (Join-Path $Root 'THIRD_PARTY_NOTICES.md') $dist -Force
Copy-Item (Join-Path $Root 'LEEME.txt') $dist -Force
Copy-Item (Join-Path $Root 'docs') $dist -Recurse -Force
Copy-Item (Join-Path $Root 'LICENSES') $dist -Recurse -Force

if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Get-ChildItem $dist | Copy-Item -Destination $stage -Recurse -Force
$zip = Join-Path $dist 'Aurora-0.1.0-bundle.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
$archive = [System.IO.Compression.ZipFile]::Open($zip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem $stage -Recurse -File | ForEach-Object {
        # Zip usa '/' aunque el sistema de archivos use '\'.
        $relative = $_.FullName.Substring($stage.Length + 1).Replace('\', '/')
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive, $_.FullName, $relative, [System.IO.Compression.CompressionLevel]::Optimal)
    }
} finally {
    $archive.Dispose()
}
Remove-Item $stage -Recurse -Force

$hashFiles = @(
    'Aurora-0.1.0.jar',
    'Aurora-0.1.0-core.jar',
    'Aurora-0.1.0-sodium.jar',
    'Aurora-0.1.0-sources.jar',
    'Aurora-0.1.0-bundle.zip',
    'README.md',
    'README.en.md',
    'THIRD_PARTY_NOTICES.md',
    'LEEME.txt'
)
$lines = foreach ($file in $hashFiles) {
    $hash = (Get-FileHash (Join-Path $dist $file) -Algorithm SHA256).Hash
    "SHA256  $file  $hash"
}
$lines | Set-Content (Join-Path $dist 'SHA256SUMS.txt') -Encoding ASCII

Write-Output "dist generado en $dist"
Get-ChildItem $dist -File | Select-Object Name, Length | Format-Table -AutoSize
