param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('en_us', 'es_es')]
    [string]$Lang
)

$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bench = Join-Path $root 'bench'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'
$options = Join-Path $root 'run\options.txt'

# Fija el idioma de Minecraft antes de arrancar. Sin BOM: Java lo lee tal cual.
$text = Get-Content $options -Raw
if ($text -match '(?m)^lang:') {
    $text = [regex]::Replace($text, '(?m)^lang:.*$', "lang:$Lang")
} else {
    $text += "`nlang:$Lang"
}
[IO.File]::WriteAllText($options, $text, (New-Object System.Text.UTF8Encoding($false)))

if (-not (Test-Path (Join-Path $mods (Split-Path $sodium -Leaf)))) {
    Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
}
Copy-Item (Join-Path $bench 'config\aurora-on.json') (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = Join-Path $bench "results\lang-$Lang.out"
$err = Join-Path $bench "results\lang-$Lang.err"
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output "Iniciando cliente con idioma $Lang..."
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
Start-Sleep -Seconds 8

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public struct RECT { public int Left, Top, Right, Bottom; }
public class AuroraLangWin32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
$handle = $mc.MainWindowHandle
[void][AuroraLangWin32]::SetForegroundWindow($handle)
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
    [void][AuroraLangWin32]::GetWindowRect($handle, [ref]$rect)
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
Start-Sleep -Seconds 6
Send-Chat '/aurora help'
Start-Sleep -Seconds 2
Save-Shot (Join-Path $bench "screenshots\lang-$Lang-help.png")
Send-Chat '/aurora status'
Start-Sleep -Seconds 2
Save-Shot (Join-Path $bench "screenshots\lang-$Lang-status.png")

$logText = Get-Content $log -Raw -ErrorAction SilentlyContinue
$status = [regex]::Matches($logText, 'Estado /aurora: .*') | Select-Object -Last 1
$errors = [regex]::Matches($logText, '(?im)^.*(ERROR|Exception|Mixin apply failed|Incompatible mod set).*$') | Select-Object -First 20
@"
LANG=$Lang
OPTIONS_LANG=$((Select-String -Path $options -Pattern '^lang:' | Select-Object -First 1).Line)
HELP_SCREENSHOT=bench/screenshots/lang-$Lang-help.png
STATUS_SCREENSHOT=bench/screenshots/lang-$Lang-status.png
STATUS_LOG=$($status.Value)
ERRORS:
$($errors.Value -join "`n")
"@ | Set-Content (Join-Path $bench "results\lang-$Lang.txt") -Encoding UTF8
Write-Output "LANG=$Lang"
Write-Output "STATUS=$($status.Value)"
Write-Output "ERRORS=$($errors.Count)"

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output "Prueba de idioma $Lang terminada."
