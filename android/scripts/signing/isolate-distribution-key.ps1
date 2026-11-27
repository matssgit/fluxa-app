<#
.SYNOPSIS
  Copia a chave que assinou as versões 1.0.0 a 1.0.2 para um keystore de distribuição exclusivo do Fluxa,
  protegido por senha forte. Opcionalmente, aposenta o debug.keystore padrão do SDK.

.DESCRIPTION
  NÃO cria uma chave nova: a chave e o certificado continuam os mesmos. Por isso os aparelhos que já têm
  o Fluxa aceitam as próximas versões como atualização normal, em qualquer Android suportado (API 30+).

  O que muda:
    - a chave passa a viver num arquivo PKCS#12 próprio, fora do repositório, com senha forte;
    - o debug.keystore padrão (senha pública "android") deixa de conter essa chave, se você usar
      -RetireDebugKeystore. O SDK cria outro debug.keystore no próximo build debug, e builds de
      desenvolvimento deixam de conseguir atualizar o app de quem o usa.

  A senha é lida da variável de ambiente FLUXA_KS_PASS e nunca é impressa.
  Nada é apagado: o debug.keystore original é MOVIDO para a pasta de destino com outro nome.

.EXAMPLE
  $env:FLUXA_KS_PASS = Read-Host "Senha do keystore de distribuição" -AsSecureString |
      ForEach-Object { [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($_)) }
  .\isolate-distribution-key.ps1 -Destination "$env:USERPROFILE\FluxaSigning"
  Remove-Item Env:FLUXA_KS_PASS
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$Destination,
    [string]$DebugKeystore = (Join-Path $env:USERPROFILE '.android/debug.keystore'),
    [string]$KeytoolPath = $(if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { 'keytool' }),
    [switch]$RetireDebugKeystore
)
$ErrorActionPreference = 'Stop'
$expected = 'AA:91:2C:9E:06:50:53:3C:9E:BE:5A:86:C0:FB:78:0A:A7:AF:03:CB:C9:A3:9E:BA:5C:90:90:AE:C1:09:8A:A9'
$alias = 'fluxa'
$target = Join-Path $Destination 'fluxa-distribution.p12'

if ([string]::IsNullOrEmpty($env:FLUXA_KS_PASS) -or $env:FLUXA_KS_PASS.Length -lt 16) {
    throw 'Defina FLUXA_KS_PASS com uma senha de pelo menos 16 caracteres (gerada por um gerenciador de senhas).'
}
if (!(Test-Path -LiteralPath $DebugKeystore -PathType Leaf)) { throw "Keystore de origem não encontrado: $DebugKeystore" }
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$destFull = [IO.Path]::GetFullPath($Destination)
if ($destFull.StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) { throw 'O destino não pode ficar dentro do repositório.' }
if (Test-Path -LiteralPath $target) { throw "Já existe $target. Nada foi alterado." }

# 1. Confere que a chave de origem é a que assinou as versões distribuídas.
$list = & $KeytoolPath -J-Duser.language=en -list -v -keystore $DebugKeystore -storepass android -alias androiddebugkey 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler o keystore de origem.' }
if ($list -notmatch [regex]::Escape("SHA256: $expected")) { throw 'A chave de origem NÃO é a que assinou o Fluxa 1.0.x. Nada foi alterado.' }

# 2. Copia para o PKCS#12 de distribuição. A senha vem do ambiente e não aparece na linha de comando.
New-Item -ItemType Directory -Force $Destination | Out-Null
& $KeytoolPath -importkeystore -noprompt `
    -srckeystore $DebugKeystore -srcstoretype JKS -srcstorepass android -srcalias androiddebugkey -srckeypass android `
    -destkeystore $target -deststoretype PKCS12 -destalias $alias `
    -deststorepass:env FLUXA_KS_PASS -destkeypass:env FLUXA_KS_PASS | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o keystore de distribuição.' }

# 3. Confere a cópia.
$check = & $KeytoolPath -J-Duser.language=en -list -v -keystore $target -storetype PKCS12 -storepass:env FLUXA_KS_PASS -alias $alias 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $check -notmatch [regex]::Escape("SHA256: $expected")) { throw 'A cópia não confere com a chave original.' }
Write-Host "Keystore de distribuição criado: $target"
Write-Host "Certificado SHA-256 conferido. Alias: $alias"

# 4. Opcional: aposenta o debug.keystore (move, não apaga).
if ($RetireDebugKeystore) {
    $retired = Join-Path $Destination ("debug.keystore.original-{0:yyyyMMdd}" -f (Get-Date))
    Move-Item -LiteralPath $DebugKeystore -Destination $retired
    Write-Host "debug.keystore original movido para $retired. O SDK criará outro no próximo build debug."
}

Write-Host ''
Write-Host 'Próximos passos obrigatórios:'
Write-Host '  1. Guardar a senha no gerenciador de senhas.'
Write-Host '  2. Copiar fluxa-distribution.p12 para DUAS mídias offline, em lugares diferentes.'
Write-Host '  3. Testar a restauração: ler o keystore a partir de uma das cópias.'
