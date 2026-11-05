$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bench = Join-Path $root 'bench'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'

# Entorno recomendado: Aurora con culling/LOD off + Sodium + bundle.
Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
Copy-Item (Join-Path $bench 'config\aurora-on.json') (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = Join-Path $bench 'results\bundle-test.out'
$err = Join-Path $bench 'results\bundle-test.err'
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output 'Iniciando cliente con bundle unificado...'
$proc = Start-Process -FilePath 'cmd.exe' -ArgumentList $gradleArgs -WorkingDirectory $root `
    -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

$deadline = (Get-Date).AddMinutes(10)
$loaded = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path $log) {
        $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
        if ($text -match 'Mixin activo: nivel') { $loaded = $true; break }
    }
}
if (-not $loaded) { throw "El mundo no cargo. Revisa $out y $err" }
Start-Sleep -Seconds 10

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public struct RECT { public int Left, Top, Right, Bottom; }
public class BundleTestWin32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
$handle = $mc.MainWindowHandle
[void][BundleTestWin32]::SetForegroundWindow($handle)
$wshell = New-Object -ComObject WScript.Shell
[void]$wshell.AppActivate($mc.Id)
Start-Sleep -Seconds 1

function Send-Chat([string]$text) {
    Set-Clipboard -Value $text
    [System.Windows.Forms.SendKeys]::SendWait('t')
    Start-Sleep -Milliseconds 700
    [System.Windows.Forms.SendKeys]::SendWait('^v')
    Start-Sleep -Milliseconds 300
    [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
    Start-Sleep -Milliseconds 1600
}

function Save-Shot([string]$path) {
    $rect = New-Object RECT
    [void][BundleTestWin32]::GetWindowRect($handle, [ref]$rect)
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bmp.Size)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

Send-Chat '/time set noon'
Send-Chat '/weather clear'
Send-Chat '/gamemode spectator'
Send-Chat '/tp @s -600 200 0 270 5'
Start-Sleep -Seconds 3
Send-Chat '/execute positioned -600 0 0 positioned over world_surface run tp @s ~ ~30 ~ 270 5'
Start-Sleep -Seconds 8
Send-Chat '/aurora compat'
Start-Sleep -Seconds 2
Save-Shot (Join-Path $bench 'screenshots\bundle-compat.png')
Send-Chat '/aurora status'
Start-Sleep -Seconds 2
Save-Shot (Join-Path $bench 'screenshots\bundle-status.png')

$text = Get-Content $log -Raw -ErrorAction SilentlyContinue
$bundle = [regex]::Matches($text, 'Bundle 26\.2 -> .*') | Select-Object -Last 1
$status = [regex]::Matches($text, 'Estado /aurora: .*') | Select-Object -Last 1
$errors = [regex]::Matches($text, '(?im)^.*(ERROR|Exception|Mixin apply failed|Incompatible mod set).*$') | Select-Object -First 30
@"
BUNDLE=$($bundle.Value)
STATUS=$($status.Value)
SCREENSHOTS=bench/screenshots/bundle-compat.png, bench/screenshots/bundle-status.png
ERRORS:
$($errors.Value -join "`n")
"@ | Set-Content (Join-Path $bench 'results\bundle-test.txt') -Encoding UTF8
Write-Output "BUNDLE=$($bundle.Value)"
Write-Output "STATUS=$($status.Value)"
Write-Output "ERRORS=$($errors.Count)"

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output 'Test de bundle terminado.'
