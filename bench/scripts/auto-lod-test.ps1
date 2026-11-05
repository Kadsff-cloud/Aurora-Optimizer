$ErrorActionPreference = 'Stop'
$root = 'D:\Cosas\AI\aurora-optimizer'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'

Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
Copy-Item 'D:\OpenCode\aurora-auto.json' (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = 'D:\OpenCode\auto-lod-test.out'
$err = 'D:\OpenCode\auto-lod-test.err'
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output 'Iniciando cliente con lodMode=auto y Sodium...'
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
Start-Sleep -Seconds 12

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class AutoLodWin32 {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@
$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
[void][AutoLodWin32]::SetForegroundWindow($mc.MainWindowHandle)
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

Send-Chat '/aurora status'
Start-Sleep -Seconds 2
Send-Chat '/aurora compat'
Start-Sleep -Seconds 2

$text = Get-Content $log -Raw -ErrorAction SilentlyContinue
$status = [regex]::Matches($text, 'Estado /aurora: .*') | Select-Object -Last 1
$sodium = [regex]::Matches($text, 'Sodium detectado: .*') | Select-Object -Last 1
$lodCapture = [regex]::Matches($text, 'LOD: primera captura.*') | Select-Object -Last 1
$summary = @"
STATUS=$($status.Value)
SODIUM=$($sodium.Value)
LOD_CAPTURE=$($lodCapture.Value)
"@
$summary | Set-Content 'D:\OpenCode\auto-lod-test.txt' -Encoding UTF8
Write-Output $summary

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output 'Test auto LOD terminado.'
