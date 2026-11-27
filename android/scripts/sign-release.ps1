<#
.SYNOPSIS
  Assina e empacota uma release pública do Fluxa a partir de app-release-unsigned.apk.

.DESCRIPTION
  Requer:
    FLUXA_KS       caminho do keystore de distribuição (fora do repositório)
    FLUXA_KS_PASS  senha do keystore (nunca impressa; repassada ao apksigner por env:)
  Gera em android/artifacts/release/ (ignorado pelo Git):
    Fluxa-<versão>.apk, Fluxa-<versão>.apk.sha256 e os relatórios de verificação.
  Recusa continuar se o certificado, o pacote, a versão ou o estado não depurável não conferirem.

.EXAMPLE
  ./gradlew :app:assembleRelease
  .\scripts\sign-release.ps1 -Version 1.0.2 -VersionCode 5
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$Version,
    [Parameter(Mandatory)] [int]$VersionCode,
    [string]$SdkRoot = $env:ANDROID_HOME,
    [string]$BuildToolsVersion = '34.0.0',
    [string]$Alias = 'fluxa',
    [string]$ExpectedCertificate = 'aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9'
)
$ErrorActionPreference = 'Stop'
$androidRoot = Split-Path $PSScriptRoot -Parent
$unsigned = Join-Path $androidRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
$outDir = Join-Path $androidRoot 'artifacts/release'
$apk = Join-Path $outDir "Fluxa-$Version.apk"
$signer = Join-Path $SdkRoot "build-tools/$BuildToolsVersion/apksigner.bat"
$aapt = Join-Path $SdkRoot "build-tools/$BuildToolsVersion/aapt.exe"

if ([string]::IsNullOrEmpty($env:FLUXA_KS) -or [string]::IsNullOrEmpty($env:FLUXA_KS_PASS)) { throw 'Defina FLUXA_KS e FLUXA_KS_PASS.' }
$repo = (Resolve-Path (Join-Path $androidRoot '..')).Path
if ([IO.Path]::GetFullPath($env:FLUXA_KS).StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) { throw 'O keystore não pode ficar dentro do repositório.' }
foreach ($required in @($unsigned, $env:FLUXA_KS, $signer, $aapt)) {
    if (!(Test-Path -LiteralPath $required -PathType Leaf)) { throw "Arquivo necessário ausente: $required" }
}
if (Test-Path -LiteralPath $apk) { throw "Já existe $apk. Remova-o conscientemente antes de assinar de novo." }
New-Item -ItemType Directory -Force $outDir | Out-Null
$sourceHash = (Get-FileHash -LiteralPath $unsigned -Algorithm SHA256).Hash

# Assina uma cópia. Sem v4 (.idsig) porque a distribuição é por download direto.
& $signer sign --ks $env:FLUXA_KS --ks-type PKCS12 --ks-key-alias $Alias --ks-pass env:FLUXA_KS_PASS --key-pass env:FLUXA_KS_PASS `
    --v4-signing-enabled false --out $apk $unsigned
if ($LASTEXITCODE -ne 0) { throw 'Falha ao assinar.' }

$signature = (& $signer verify --verbose --print-certs $apk 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) { Remove-Item $apk; throw 'Assinatura inválida.' }
$cert = [regex]::Match($signature, 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)').Groups[1].Value.ToLowerInvariant()
if ($cert -ne $ExpectedCertificate) { Remove-Item $apk; throw 'Certificado diferente do esperado. APK removido.' }
$signature | Set-Content (Join-Path $outDir "Fluxa-$Version.signature.txt") -Encoding utf8

$badging = (& $aapt dump badging $apk) -join "`n"
if ($badging -notlike "*name='com.fluxa.personal' versionCode='$VersionCode' versionName='$Version'*" -or $badging -match 'application-debuggable') {
    Remove-Item $apk; throw 'Pacote, versão ou modo de depuração inesperados. APK removido.'
}
$badging | Set-Content (Join-Path $outDir "Fluxa-$Version.badging.txt") -Encoding utf8
if ((Get-FileHash -LiteralPath $unsigned -Algorithm SHA256).Hash -ne $sourceHash) { throw 'O APK sem assinatura mudou durante o processo.' }

& python (Join-Path $PSScriptRoot 'audit-release-apk.py') $apk | Out-Null
if ($LASTEXITCODE -ne 0) { Remove-Item $apk; throw 'A auditoria de conteúdo falhou. APK removido.' }

$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  Fluxa-$Version.apk" | Set-Content (Join-Path $outDir "Fluxa-$Version.apk.sha256") -Encoding ascii -NoNewline
[ordered]@{ file = "Fluxa-$Version.apk"; bytes = (Get-Item $apk).Length; sha256 = $hash; certificateSha256 = $cert } | ConvertTo-Json
