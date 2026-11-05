param(
    [string]$Artifacts = 'D:\Cosas\AI\opencode\projects\aurora-optimizer\artifacts'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$expected = @{
    '5ZwdcRci-pMWERcSu.jar' = '5836b089072c0fd796613d42c3bd01c7e401b6ad53de80328bb9d74d585de9be0ff0cfc4d6ffa1ed41008750573b0525eff8c064162a2f724ad16817a8fdaf22'
    'g96Z4WVZ-JmPs4Wie.jar' = 'f91c409d4ce68d027ac0a11b6d2828c5f6d057e749ff0ce596f3a84a65fc1e3905b08f4d97a21a7302e2bfaf4ff865907272bd9c7ffa8487701b22e40dfe2f97'
    '51shyZVL-D5oVCouK.jar' = '9c331ba5f2ce9c322c41aa577f1029f3420d5507d0d942aadbb43fb9bd82e7da6f72e3a0b26c4ad96394669a99a7feb512b5119b3095fde2e0a437bf68c08b40'
    '9s6osm5g-Nv3xnWXd.jar' = '37b1e402f0df5a383656e21a38ee18cdd15cb4ba3fb62fbeba82ef4b959a4479fc32718ac0d9d154a7d9104c5f7315bfa67dbeced0b8ff240b8039d4848d5df1'
    'Wnxd13zP-dEMopoOJ.jar' = 'a57044f0c9a07b19cd38b85528fd6b3958600c3ed1568d8a7ce32a7a473570362861ef433be4f23b02a43c05d658e9a5a71f9218fbdb963ae76a1714a0439fbe'
    'gvQqBUqZ-f7vZ0VWU.jar' = '148b638f3c6229fbaf487120a2344a0af5e411a5aa6533d5db9d75da0a8c0d8304f63eb4cca13f4d03b2c9b4c23d559dd74c1d832422ef8a3087bd005e62a8bd'
    'uXXizFIs-d5ddUdiB.jar' = 'd81fa97e11784c19d42f89c2f433831d007603dd7193cee45fa177e4a6a9c52b384b198586e04a0f7f63cd996fed713322578bde9a8db57e1188854ae5cbe584'
    'QSvN5pBU-QQzYehRo.jar' = 'a0f7d92db42cfdb44cd815b82f8bbb68528b54531836b4ff27ac724f2ac3cc3ab51b5b5b9798c0ccd8d5aa03cbf386183d973d731d705d896d330bc3d618dc4e'
    'ETlrkaYF-ZM0HUAhK.jar' = 'ccd86e34dc252d59ec8ef5bbba0fb7f23f04facc4028e07b0bb6e8e0cf19b27c94a4c355886f9addd0a472ede0b21d19f95626fffb0b1ee4ce3d005b210e4cce'
}

function Get-Sha512([byte[]]$bytes) {
    $sha = [System.Security.Cryptography.SHA512]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($bytes)) -replace '-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

$jars = @(
    @{ File = 'Aurora-0.1.0.jar';        ExpectedNested = 9; ExpectedEnvironment = '*';      ExpectSodium = $false },
    @{ File = 'Aurora-0.1.0-core.jar';   ExpectedNested = 0; ExpectedEnvironment = '*';      ExpectSodium = $false },
    @{ File = 'Aurora-0.1.0-sodium.jar'; ExpectedNested = 9; ExpectedEnvironment = 'client'; ExpectSodium = $true }
)

$failed = $false
foreach ($spec in $jars) {
    $path = Join-Path $Artifacts $spec.File
    if (-not (Test-Path $path)) {
        Write-Output "FALTA $($spec.File)"
        $failed = $true
        continue
    }

    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $metaEntry = $zip.Entries | Where-Object { $_.FullName -eq 'fabric.mod.json' }
        if (-not $metaEntry) { throw "sin fabric.mod.json" }
        $reader = New-Object IO.StreamReader($metaEntry.Open())
        $meta = $reader.ReadToEnd() | ConvertFrom-Json
        $reader.Dispose()

        $nested = @($zip.Entries | Where-Object { $_.FullName -like 'META-INF/jars/*.jar' })
        $sodiumInside = @($nested | Where-Object { $_.FullName -match 'AANobbMI|sodium' }).Count -gt 0
        $hasSodiumDep = $null -ne $meta.depends.sodium
        $langs = @($zip.Entries | Where-Object { $_.FullName -like 'assets/aurora/lang/*.json' } | ForEach-Object { $_.FullName })
        $hasEn = $langs -contains 'assets/aurora/lang/en_us.json'
        $hasEs = $langs -contains 'assets/aurora/lang/es_es.json'

        $status = 'OK'
        if ($nested.Count -ne $spec.ExpectedNested) { $status = "FALLO nested=$($nested.Count)"; $failed = $true }
        if ($meta.environment -ne $spec.ExpectedEnvironment) { $status = "FALLO env=$($meta.environment)"; $failed = $true }
        if ($hasSodiumDep -ne $spec.ExpectSodium) { $status = "FALLO sodiumDep=$hasSodiumDep"; $failed = $true }
        if ($sodiumInside) { $status = 'FALLO Sodium empaquetado'; $failed = $true }
        if (-not ($hasEn -and $hasEs)) { $status = 'FALLO idiomas'; $failed = $true }

        Write-Output "=== $($spec.File) -> $status ==="
        Write-Output "environment=$($meta.environment) | depends.sodium=$(if ($hasSodiumDep) { $meta.depends.sodium } else { '(ausente)' }) | nested=$($nested.Count) | sodium_inside=$sodiumInside | langs=$($langs -join ',')"

        foreach ($entry in $nested) {
            $stream = $entry.Open()
            $ms = New-Object IO.MemoryStream
            $stream.CopyTo($ms)
            $stream.Dispose()
            $hash = Get-Sha512 $ms.ToArray()
            $ms.Dispose()
            $name = Split-Path $entry.FullName -Leaf
            $expectedHash = $expected[$name]
            if (-not $expectedHash) {
                Write-Output "  $name -> sin hash esperado"
                $failed = $true
            } elseif ($hash -eq $expectedHash) {
                Write-Output "  $name -> SHA-512 MATCH"
            } else {
                Write-Output "  $name -> SHA-512 MISMATCH"
                $failed = $true
            }
        }
    } finally {
        $zip.Dispose()
    }
}

if ($failed) {
    Write-Output 'VERIFICACION: FALLOS'
    exit 1
}
Write-Output 'VERIFICACION: TODO OK'
