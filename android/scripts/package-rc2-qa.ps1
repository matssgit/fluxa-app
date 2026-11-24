[CmdletBinding()]
param(
    [string]$SdkRoot = $env:ANDROID_HOME,
    [string]$QaKeyStore = (Join-Path $env:USERPROFILE '.android/debug.keystore'),
    [string]$BuildToolsVersion = '34.0.0'
)
$ErrorActionPreference = 'Stop'
$androidRoot = Split-Path $PSScriptRoot -Parent
$unsigned = Join-Path $androidRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
$outputDir = Join-Path $androidRoot 'artifacts/rc2'
$apk = Join-Path $outputDir 'Fluxa-1.0.0-RC2-s23.apk'
if ([string]::IsNullOrWhiteSpace($SdkRoot)) { throw 'Defina ANDROID_HOME ou -SdkRoot.' }
$signer = Join-Path $SdkRoot "build-tools/$BuildToolsVersion/apksigner.bat"
$aapt = Join-Path $SdkRoot "build-tools/$BuildToolsVersion/aapt.exe"
foreach ($required in @($unsigned, $QaKeyStore, $signer, $aapt)) {
    if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Arquivo necessario ausente: $required" }
}
New-Item -ItemType Directory -Force $outputDir | Out-Null
$sourceHash = (Get-FileHash -LiteralPath $unsigned -Algorithm SHA256).Hash
# Existing standard Android debug certificate, for disposable/local QA only.
# Never creates a key, changes release signingConfig or packages the private keystore.
& $signer sign --ks $QaKeyStore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --v4-signing-enabled false --out $apk $unsigned
if ($LASTEXITCODE -ne 0) { throw 'Falha ao assinar a copia local de QA.' }
$signature = (& $signer verify --verbose --print-certs $apk 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Assinatura invalida.' }
$signature | Set-Content (Join-Path $outputDir 'signature-verification.txt') -Encoding utf8
$badging = (& $aapt dump badging $apk) -join "`n"
if ($LASTEXITCODE -ne 0 -or $badging -notlike "*name='com.fluxa.personal' versionCode='2' versionName='1.0.0'*" -or $badging -match 'application-debuggable') {
    throw 'Contrato de pacote/versao/release inesperado.'
}
$badging | Set-Content (Join-Path $outputDir 'apk-badging.txt') -Encoding utf8
$resourceTable = (& $aapt dump --values resources $apk) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Falha ao ler tabela de recursos.' }
function Find-XmlResource([string]$name) {
    $pattern = '(?m)^\s*resource [^\r\n]+:xml/' + [regex]::Escape($name) + ': [^\r\n]+\r?\n\s*\(string8\) "([^"]+)"'
    $match = [regex]::Match($resourceTable, $pattern)
    if (!$match.Success) { throw "Recurso XML ausente: $name" }
    return $match.Groups[1].Value
}
$backupPath = Find-XmlResource 'backup_rules'
$extractionPath = Find-XmlResource 'data_extraction_rules'
foreach ($item in @(
    @{Name='apk-permissions.txt';Args=@('dump','permissions',$apk)},
    @{Name='apk-manifest.txt';Args=@('dump','xmltree',$apk,'AndroidManifest.xml')},
    @{Name='apk-backup-rules.txt';Args=@('dump','xmltree',$apk,$backupPath)},
    @{Name='apk-extraction-rules.txt';Args=@('dump','xmltree',$apk,$extractionPath)}
)) {
    $params = $item.Args
    & $aapt @params | Set-Content (Join-Path $outputDir $item.Name) -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw "Falha na inspecao $($item.Name)" }
}
if ((Get-FileHash -LiteralPath $unsigned -Algorithm SHA256).Hash -ne $sourceHash) { throw 'O unsigned original foi modificado.' }
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$certificate = [regex]::Match($signature, 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)').Groups[1].Value
$expectedCertificate = 'aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9'
if ($certificate.ToLowerInvariant() -ne $expectedCertificate) { throw 'Certificado diferente da RC1. Nao instalar nem desinstalar a RC1.' }
$metadata = [ordered]@{
    filename = [IO.Path]::GetFileName($apk)
    bytes = (Get-Item -LiteralPath $apk).Length
    sha256 = $hash
    sourceUnsignedSha256 = $sourceHash.ToLowerInvariant()
    certificateSha256 = $certificate.ToLowerInvariant()
    signing = 'Existing local Android debug certificate; non-debuggable release; QA only'
}
$metadata | ConvertTo-Json | Set-Content (Join-Path $outputDir 'APK-METADATA.json') -Encoding utf8
"$hash  $([IO.Path]::GetFileName($apk))" | Set-Content (Join-Path $outputDir 'SHA256SUMS.txt') -Encoding ascii
$metadata | ConvertTo-Json
