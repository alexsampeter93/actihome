<#
    empaquetar.ps1 — Genera ActiHome.exe con su icono.

        .\empaquetar.ps1

    Deja la aplicación lista en dist\ActiHome\ActiHome.exe. Ese ejecutable se
    abre con doble clic, lleva el icono propio y NO necesita que quien lo use
    tenga Java instalado: jpackage empaqueta dentro una copia recortada del
    entorno de ejecución. Tampoco necesita base de datos: usa H2 embebida.

    Requiere un JDK 14 o superior (jpackage viene con él). Aquí se usa el 17.
#>

$ErrorActionPreference = "Stop"
$raiz = $PSScriptRoot
$nombre = "ActiHome"
$version = "1.0.0"

# ---------------------------------------------------------------------------
# 1. Construir el jar ejecutable
# ---------------------------------------------------------------------------
Write-Host "==> Compilando y empaquetando..." -ForegroundColor Cyan
& "$raiz\mvnw.cmd" -q package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Fallo al empaquetar" }

$jar = Get-ChildItem "$raiz\target\*.jar" | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*.original" } | Select-Object -First 1
Write-Host "    $($jar.Name)  ($([math]::Round($jar.Length/1MB,1)) MB)"

# Carpeta de preparación con SOLO el jar.
#
# jpackage copia al ejecutable todo lo que encuentre en la carpeta de entrada, y
# si se le pasa "target" directamente se lleva también las clases sueltas, los
# tests compilados, los informes de Surefire y los fuentes generados. Funciona,
# pero reparte basura en la distribución y engorda la carpeta final.
$preparacion = "$raiz\target\jpackage-input"
if (Test-Path $preparacion) { Remove-Item $preparacion -Recurse -Force }
New-Item -ItemType Directory -Path $preparacion | Out-Null
Copy-Item $jar.FullName -Destination $preparacion

# ---------------------------------------------------------------------------
# 2. Construir el .ico a partir de los PNG del icono
#
# Windows exige formato ICO para el icono de un ejecutable, y no tenemos uno:
# solo PNG. Un ICO es un contenedor sencillo —una cabecera, una entrada por
# tamaño y los datos de cada imagen— y desde Windows Vista admite guardar cada
# imagen como PNG dentro, así que se puede construir concatenando bytes sin
# necesidad de ninguna herramienta externa.
# ---------------------------------------------------------------------------
Write-Host "==> Construyendo el icono..." -ForegroundColor Cyan

$tamanos = 16, 32, 48, 64, 128, 256
$imagenes = @()
foreach ($t in $tamanos) {
    $ruta = "$raiz\src\main\resources\images\brand\actihome-icon-$t.png"
    if (-not (Test-Path $ruta)) { throw "Falta $ruta" }
    $imagenes += , @{ Tamano = $t; Bytes = [System.IO.File]::ReadAllBytes($ruta) }
}

$flujo = New-Object System.IO.MemoryStream
$escritor = New-Object System.IO.BinaryWriter($flujo)

# ICONDIR: reservado, tipo (1 = icono), número de imágenes
$escritor.Write([UInt16]0)
$escritor.Write([UInt16]1)
$escritor.Write([UInt16]$imagenes.Count)

# Cada entrada del directorio ocupa 16 bytes; los datos van después de todas.
$desplazamiento = 6 + 16 * $imagenes.Count
foreach ($img in $imagenes) {
    # 256 se codifica como 0: el campo es de un solo byte y no llega a 256.
    $lado = if ($img.Tamano -ge 256) { 0 } else { $img.Tamano }
    $escritor.Write([Byte]$lado)          # ancho
    $escritor.Write([Byte]$lado)          # alto
    $escritor.Write([Byte]0)              # colores de la paleta (0 = sin paleta)
    $escritor.Write([Byte]0)              # reservado
    $escritor.Write([UInt16]1)            # planos
    $escritor.Write([UInt16]32)           # bits por píxel
    $escritor.Write([UInt32]$img.Bytes.Length)
    $escritor.Write([UInt32]$desplazamiento)
    $desplazamiento += $img.Bytes.Length
}

foreach ($img in $imagenes) { $escritor.Write($img.Bytes) }

$escritor.Flush()
$ico = "$raiz\target\actihome.ico"
[System.IO.File]::WriteAllBytes($ico, $flujo.ToArray())
$escritor.Dispose(); $flujo.Dispose()
Write-Host "    $ico  ($($imagenes.Count) tamaños)"

# ---------------------------------------------------------------------------
# 3. jpackage
# ---------------------------------------------------------------------------
Write-Host "==> Generando el ejecutable..." -ForegroundColor Cyan

# jpackage vive en el JDK, y lo que suele estar en el PATH es el intérprete de
# Java (el "javapath" de Oracle), que no lo incluye. Se busca en JAVA_HOME y en
# las ubicaciones habituales de instalación.
$jpackage = $null
$candidatos = @()
if ($env:JAVA_HOME) { $candidatos += "$env:JAVA_HOME\bin\jpackage.exe" }
foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium")) {
    if (Test-Path $base) {
        Get-ChildItem $base -Directory | Sort-Object Name -Descending | ForEach-Object {
            $candidatos += (Join-Path $_.FullName "bin\jpackage.exe")
        }
    }
}
$candidatos += "jpackage"

foreach ($c in $candidatos) {
    if ($c -eq "jpackage") {
        if (Get-Command jpackage -ErrorAction SilentlyContinue) { $jpackage = "jpackage"; break }
    } elseif (Test-Path $c) { $jpackage = $c; break }
}

if (-not $jpackage) { throw "No se encontro jpackage. Necesitas un JDK 14 o superior instalado." }
Write-Host "    usando $jpackage"

$destino = "$raiz\dist"
if (Test-Path "$destino\$nombre") { Remove-Item "$destino\$nombre" -Recurse -Force }
if (-not (Test-Path $destino)) { New-Item -ItemType Directory -Path $destino | Out-Null }

# --main-class explícito: el jar de Spring Boot arranca a través de JarLauncher,
# que es quien sabe leer las dependencias empaquetadas dentro del propio jar.
# Sin indicarlo, jpackage intentaría llamar directamente a la clase de la
# aplicación y no encontraría ninguna de sus librerías.
& $jpackage `
    --type app-image `
    --name $nombre `
    --app-version $version `
    --input $preparacion `
    --main-jar $jar.Name `
    --main-class org.springframework.boot.loader.JarLauncher `
    --icon $ico `
    --dest $destino `
    --vendor "CocoBrain" `
    --description "Gestion y reserva de alojamientos turisticos" `
    --java-options "-Dfile.encoding=UTF-8"

if ($LASTEXITCODE -ne 0) { throw "jpackage fallo" }

$exe = "$destino\$nombre\$nombre.exe"
$peso = [math]::Round(((Get-ChildItem "$destino\$nombre" -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 0)
Write-Host ""
Write-Host "Listo: $exe" -ForegroundColor Green
Write-Host "Carpeta completa: $peso MB (incluye el entorno de ejecucion de Java)"
