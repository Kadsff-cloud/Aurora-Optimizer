param(
    [Parameter(Mandatory = $true)][string]$Label,
    [Parameter(Mandatory = $true)][ValidateSet('aurora-off', 'aurora-on', 'aurora-lod', 'aurora-lod-captured', 'aurora-lod-worldgen', 'aurora-lod-worldgen-conservative', 'sodium-off', 'sodium-on', 'sodium-lod', 'sodium-lod-captured', 'sodium-lod-worldgen', 'sodium-lod-worldgen-conservative')][string]$Mode,
    [int]$Seconds = 30,
    [int]$Warmup = 60,
    [int]$SceneX = -600,
    [int]$SceneZ = 0,
    [int]$Yaw = 270,
    [int]$Pitch = 25
)

$ErrorActionPreference = 'Stop'
$root = 'D:\Cosas\AI\aurora-optimizer'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'
$config = switch ($Mode) {
    'aurora-off'           { 'D:\OpenCode\aurora-off.json' }
    'sodium-off'           { 'D:\OpenCode\aurora-off.json' }
    'aurora-lod'           { 'D:\OpenCode\aurora-lod.json' }
    'sodium-lod'           { 'D:\OpenCode\aurora-lod.json' }
    'aurora-lod-captured'  { 'D:\OpenCode\aurora-lod-captured.json' }
    'sodium-lod-captured'  { 'D:\OpenCode\aurora-lod-captured.json' }
    'aurora-lod-worldgen'  { 'D:\OpenCode\aurora-lod-worldgen.json' }
    'sodium-lod-worldgen'  { 'D:\OpenCode\aurora-lod-worldgen.json' }
    'aurora-lod-worldgen-conservative' { 'D:\OpenCode\aurora-lod-worldgen-conservative.json' }
    'sodium-lod-worldgen-conservative' { 'D:\OpenCode\aurora-lod-worldgen-conservative.json' }
    default                { 'D:\OpenCode\aurora-on.json' }
}
$useSodium = $Mode -like 'sodium-*'

# Mods: Sodium solo en los modos sodium-*.
Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
if ($useSodium) {
    Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
}
Copy-Item $config (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = "D:\OpenCode\bench-$Label.out"
$err = "D:\OpenCode\bench-$Label.err"
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output "Starting $Label ($Mode), Sodium=$useSodium..."
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
public class BenchWin32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
$handle = $mc.MainWindowHandle
[void][BenchWin32]::SetForegroundWindow($handle)
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
    Start-Sleep -Milliseconds 1400
}

function Save-Shot([string]$path) {
    $rect = New-Object RECT
    [void][BenchWin32]::GetWindowRect($handle, [ref]$rect)
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bmp.Size)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

# Preparacion: mundo fijo, mediodia, sin clima, espectador y camara fija.
Send-Chat '/time set noon'
Send-Chat '/weather clear'
Send-Chat '/gamemode spectator'
Send-Chat '/gamerule advance_time false'
Send-Chat '/gamerule advance_weather false'
Send-Chat '/gamerule spawn_mobs false'
Send-Chat '/gamerule spawn_monsters false'
Send-Chat '/gamerule random_tick_speed 0'
Send-Chat "/tp @s $SceneX 200 $SceneZ $Yaw $Pitch"
Start-Sleep -Seconds 8
Send-Chat "/execute positioned $SceneX 0 $SceneZ positioned over world_surface run tp @s ~ ~30 ~ $Yaw $Pitch"
Write-Output "Escena fijada en ($SceneX,$SceneZ) yaw=$Yaw pitch=$Pitch. Calentando ${Warmup}s..."
Start-Sleep -Seconds $Warmup

Save-Shot "D:\OpenCode\bench-$Label.png"
Send-Chat '/aurora status'
Start-Sleep -Seconds 2
Send-Chat "/aurora bench $Seconds"

$deadline = (Get-Date).AddMinutes(3)
$result = $null
$text = ''
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
    $matches = [regex]::Matches($text, 'Aurora bench: .*')
    if ($matches.Count -gt 0) {
        $result = $matches[$matches.Count - 1].Value
        break
    }
    if ($text -notmatch 'Benchmark: inicio') {
        Write-Output 'El comando no arranco; reintentando...'
        Send-Chat "/aurora bench $Seconds"
    }
}

if (-not $result) { throw "No se obtuvo resultado del benchmark. Revisa $log" }

$statusMatches = [regex]::Matches($text, 'Estado /aurora: .*')
$status = if ($statusMatches.Count -gt 0) { $statusMatches[$statusMatches.Count - 1].Value } else { '' }
$summary = @"
LABEL=$Label
MODE=$Mode
SODIUM=$useSodium
SCENE=$SceneX,$SceneZ yaw=$Yaw pitch=$Pitch
WARMUP=$Warmup
SECONDS=$Seconds
RESULT=$result
STATUS=$status
SCREENSHOT=D:\OpenCode\bench-$Label.png
"@
$summary | Set-Content "D:\OpenCode\bench-$Label.txt" -Encoding UTF8
Write-Output $summary

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output "Cliente cerrado: $Label"
