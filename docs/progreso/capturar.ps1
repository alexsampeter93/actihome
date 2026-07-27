<#
    capturar.ps1 — Captura la ventana de ActiHome para el registro visual del rediseño.

    Busca la ventana por su título, la trae al frente y guarda solo esa ventana
    (no el escritorio entero) en docs/progreso/.

    Uso, con la aplicación ya arrancada:
        .\docs\progreso\capturar.ps1 -Nombre "fase2-login"

    Convención de nombres: faseN-pantalla.png  (ej. fase3-catalogo.png)
    Para el estado previo al rediseño se usa el prefijo "antes-".
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Nombre,

    [string]$Titulo = "Actihome",

    [string]$Destino = (Join-Path $PSScriptRoot "")
)

Add-Type -AssemblyName System.Drawing, System.Windows.Forms

# Localizar la ventana real de la aplicación.
#
# Dos trampas de Java en Windows, aprendidas a base de fallar:
#   1. FindWindow no localiza de forma fiable las ventanas de AWT/Swing.
#   2. Get-Process .MainWindowHandle devuelve a menudo una ventana "propietaria"
#      oculta que Java aparca muy fuera de pantalla (coordenadas ~ -25000).
# Por eso se enumeran todas las ventanas de nivel superior del proceso y se
# escoge la visible, con título y de mayor superficie.
$sig = @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public class Win {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int max);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr param);
    public delegate bool EnumProc(IntPtr h, IntPtr param);
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }

    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
    public const int SW_RESTORE = 9;

    // PrintWindow le pide a la ventana que se dibuje sobre un lienzo propio, en vez
    // de copiar pixeles de la pantalla. Es la diferencia entre capturar la ventana
    // y capturar "lo que hubiera en ese trozo de monitor": sin esto, cualquier cosa
    // que tape la app (otra aplicacion, un menu, un juego) sale en la captura.
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
    public const uint PW_RENDERFULLCONTENT = 2;

    public static IntPtr Buscar(uint targetPid, string titulo) {
        IntPtr encontrada = IntPtr.Zero;
        EnumWindows(delegate(IntPtr h, IntPtr p) {
            uint pid; GetWindowThreadProcessId(h, out pid);
            if (pid != targetPid || !IsWindowVisible(h)) return true;
            StringBuilder sb = new StringBuilder(256);
            GetWindowText(h, sb, 256);
            if (sb.ToString() == titulo) { encontrada = h; return false; }
            return true;
        }, IntPtr.Zero);
        return encontrada;
    }
}
'@
if (-not ("Win" -as [type])) { Add-Type -TypeDefinition $sig }

$h = [IntPtr]::Zero
foreach ($p in (Get-Process java -ErrorAction SilentlyContinue)) {
    $candidato = [Win]::Buscar([uint32]$p.Id, $Titulo)
    if ($candidato -ne [IntPtr]::Zero) { $h = $candidato; break }
}
if ($h -eq [IntPtr]::Zero) {
    Write-Error "No se encontró ninguna ventana visible titulada '$Titulo'. ¿Está la app arrancada?"
    exit 1
}

# Restaurar antes de medir: una ventana minimizada devuelve el rectángulo
# centinela -32000,-32000 en lugar de su posición real.
[Win]::ShowWindow($h, [Win]::SW_RESTORE) | Out-Null
[Win]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Milliseconds 600

$r = New-Object Win+RECT
[Win]::GetWindowRect($h, [ref]$r) | Out-Null

if ($r.Left -lt -10000) {
    Write-Error "La ventana sigue minimizada; no se puede capturar."
    exit 1
}

$w = $r.Right - $r.Left
$hh = $r.Bottom - $r.Top

$bmp = New-Object System.Drawing.Bitmap $w, $hh
$g = [System.Drawing.Graphics]::FromImage($bmp)

# Se pide a la propia ventana que se pinte. Si falla (algunos controles no lo
# soportan), se recurre a copiar de pantalla, que exige que nada la tape.
$hdc = $g.GetHdc()
$ok = [Win]::PrintWindow($h, $hdc, [Win]::PW_RENDERFULLCONTENT)
$g.ReleaseHdc($hdc)

if (-not $ok) {
    Write-Warning "PrintWindow no funciono en esta ventana; se copia de pantalla. Asegurate de que nada la tapa."
    $g.CopyFromScreen($r.Left, $r.Top, 0, 0, $bmp.Size)
}

$ruta = Join-Path $Destino "$Nombre.png"
$bmp.Save($ruta, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()

Write-Output "Captura guardada: $ruta  ($w x $hh)"
