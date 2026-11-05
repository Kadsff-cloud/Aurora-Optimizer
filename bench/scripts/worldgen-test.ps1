$ErrorActionPreference = 'Stop'
$root = 'D:\Cosas\AI\aurora-optimizer'
$log = Join-Path $root 'run\logs\latest.log'
$mods = Join-Path $root 'run\mods'
$sodium = 'D:\OpenCode\sodium-fabric-0.9.2+mc26.2.jar'

Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $sodium (Join-Path $mods (Split-Path $sodium -Leaf)) -Force
Copy-Item 'D:\OpenCode\aurora-worldgen.json' (Join-Path $root 'run\config\aurora.json') -Force
if (Test-Path $log) { Remove-Item $log -Force }

$out = 'D:\OpenCode\worldgen-test.out'
$err = 'D:\OpenCode\worldgen-test.err'
$gradleArgs = '/c .\gradlew.bat runClient --no-daemon --console=plain -PquickPlayWorld=AuroraBench -PbenchWidth=1920 -PbenchHeight=1080'
Write-Output 'Iniciando cliente con lodMode=aurora, worldgen LOD y Sodium...'
$proc = Start-Process -FilePath 'cmd.exe' -ArgumentList $gradleArgs -WorkingDirectory $root `
    -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

$deadline = (Get-Date).AddMinutes(10)
$loaded = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path $log) {
        $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
        if ($text -match 'Mixin activo: nivel' -or $text -match 'LOD worldgen: generando anillos') { $loaded = $true; break }
    }
}
if (-not $loaded) { throw "El mundo no cargo. Revisa $out y $err" }
Start-Sleep -Seconds 30

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WorldgenWin32 {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@
$mc = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $mc) { throw 'No encuentro la ventana de Minecraft' }
[void][WorldgenWin32]::SetForegroundWindow($mc.MainWindowHandle)
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
    Start-Sleep -Milliseconds 1800
}

Send-Chat '/aurora status'
Start-Sleep -Seconds 3
Send-Chat '/aurora compat'
Start-Sleep -Seconds 2

# Salir al titulo con teclado: Esc y luego buscar el boton inferior.
[System.Windows.Forms.SendKeys]::SendWait('{ESC}')
Start-Sleep -Seconds 2
for ($i = 0; $i -lt 6; $i++) {
    [System.Windows.Forms.SendKeys]::SendWait('{TAB}')
    Start-Sleep -Milliseconds 250
}
[System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
Start-Sleep -Seconds 8

$text = Get-Content $log -Raw -ErrorAction SilentlyContinue
$status = [regex]::Matches($text, 'Estado /aurora: .*') | Select-Object -Last 1
$firstGen = [regex]::Matches($text, 'LOD worldgen: primer chunk generado.*') | Select-Object -Last 1
$genStart = [regex]::Matches($text, 'LOD worldgen: generando anillos.*') | Select-Object -Last 1
$capture = [regex]::Matches($text, 'LOD: primera captura.*') | Select-Object -Last 1
$leak = [regex]::Matches($text, 'Aurora leak guard: nivel descargado.*') | Select-Object -Last 1
$errors = [regex]::Matches($text, 'LOD worldgen: fallo.*|No se pudo.*|Exception.*') | Select-Object -Last 5
$summary = @"
STATUS=$($status.Value)
WORLDGEN_START=$($genStart.Value)
FIRST_GENERATED=$($firstGen.Value)
LOD_CAPTURE=$($capture.Value)
LEAK_GUARD=$($leak.Value)
ERRORS=$($errors.Value -join ' | ')
"@
$summary | Set-Content 'D:\OpenCode\worldgen-test.txt' -Encoding UTF8
Write-Output $summary

[void]$mc.CloseMainWindow()
Start-Sleep -Seconds 8
if (-not $proc.HasExited) { [void]$proc.WaitForExit(120000) }
Write-Output 'Test worldgen terminado.'
