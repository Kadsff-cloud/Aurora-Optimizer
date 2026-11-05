$ErrorActionPreference = 'Stop'
$root = 'D:\Cosas\AI\aurora-optimizer'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'

Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
Copy-Item 'D:\OpenCode\aurora-off.json' (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = 'D:\OpenCode\cleanup-entities.out'
$err = 'D:\OpenCode\cleanup-entities.err'
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output 'Iniciando cliente para limpiar entidades...'
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
public class CleanupWin32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
$handle = $mc.MainWindowHandle
[void][CleanupWin32]::SetForegroundWindow($handle)
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
    [void][CleanupWin32]::GetWindowRect($handle, [ref]$rect)
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bmp.Size)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

Send-Chat '/gamemode spectator'
Send-Chat '/function aurora:clear_entities'
Start-Sleep -Seconds 2
Send-Chat '/execute store result score #pigs aurora_count if entity @e[type=minecraft:pig]'
Send-Chat '/execute store result score #golems aurora_count if entity @e[type=minecraft:iron_golem]'
Send-Chat '/scoreboard players get #pigs aurora_count'
Send-Chat '/scoreboard players get #golems aurora_count'
[System.Windows.Forms.SendKeys]::SendWait('t')
Start-Sleep -Seconds 2
Save-Shot 'D:\OpenCode\cleanup-entities.png'
[System.Windows.Forms.SendKeys]::SendWait('{ESC}')
Start-Sleep -Seconds 1
Send-Chat '/aurora status'
Start-Sleep -Seconds 2

$text = Get-Content $log -Raw -ErrorAction SilentlyContinue
$status = [regex]::Matches($text, 'Estado /aurora: .*') | Select-Object -Last 1
$statusText = if ($status) { $status.Value } else { '' }
@"
CLEANUP=entities
SCREENSHOT=cleanup-entities.png
STATUS=$statusText
"@ | Set-Content 'D:\OpenCode\cleanup-entities.txt' -Encoding UTF8
Write-Output $statusText

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output 'Limpieza terminada.'
