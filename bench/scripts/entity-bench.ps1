param(
    [Parameter(Mandatory = $true)][string]$Label,
    [Parameter(Mandatory = $true)][ValidateSet('sodium-on', 'sodium-off')][string]$Mode,
    [int]$Seconds = 20,
    [int]$Warmup = 45,
    [int]$CameraY = 140,
    [int]$Pitch = 0
)

$ErrorActionPreference = 'Stop'
$root = 'D:\Cosas\AI\aurora-optimizer'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'
$config = if ($Mode -eq 'sodium-on') { 'D:\OpenCode\aurora-on.json' } else { 'D:\OpenCode\aurora-off.json' }

Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
Copy-Item $config (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = "D:\OpenCode\entity-bench-$Label.out"
$err = "D:\OpenCode\entity-bench-$Label.err"
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output "Starting $Label ($Mode), cameraY=$CameraY pitch=$Pitch..."
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
public class EntityBenchWin32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
$handle = $mc.MainWindowHandle
[void][EntityBenchWin32]::SetForegroundWindow($handle)
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
    [void][EntityBenchWin32]::GetWindowRect($handle, [ref]$rect)
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
Send-Chat '/gamerule advance_time false'
Send-Chat '/gamerule advance_weather false'
Send-Chat '/gamerule spawn_mobs false'
Send-Chat '/gamerule spawn_monsters false'
Send-Chat '/gamerule random_tick_speed 0'
Send-Chat "/tp @s -600 $CameraY 0 270 $Pitch"
Write-Output "Camara fijada en (-600,$CameraY,0) yaw=270 pitch=$Pitch. Calentando ${Warmup}s..."
Start-Sleep -Seconds $Warmup

Save-Shot "D:\OpenCode\entity-bench-$Label.png"
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
SODIUM=True
SCENE=-600,$CameraY,0 yaw=270 pitch=$Pitch
WARMUP=$Warmup
SECONDS=$Seconds
RESULT=$result
STATUS=$status
SCREENSHOT=D:\OpenCode\entity-bench-$Label.png
"@
$summary | Set-Content "D:\OpenCode\entity-bench-$Label.txt" -Encoding UTF8
Write-Output $summary

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output "Cliente cerrado: $Label"
