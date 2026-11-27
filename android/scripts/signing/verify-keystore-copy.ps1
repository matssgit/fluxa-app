<#
.SYNOPSIS
  Teste de restauração: confere se uma cópia do keystore de distribuição do Fluxa abre com a senha e contém o
  certificado esperado. A senha é pedida com entrada mascarada e não é gravada.

.EXAMPLE
  .\verify-keystore-copy.ps1 -Keystore E:\FluxaSigning\fluxa-distribution.p12
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$Keystore,
    [string]$KeytoolPath = $(if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { 'keytool' })
)
$ErrorActionPreference = 'Stop'
$expected = 'AA:91:2C:9E:06:50:53:3C:9E:BE:5A:86:C0:FB:78:0A:A7:AF:03:CB:C9:A3:9E:BA:5C:90:90:AE:C1:09:8A:A9'
if (!(Test-Path -LiteralPath $Keystore -PathType Leaf)) { throw "Arquivo não encontrado: $Keystore" }
$secure = Read-Host 'Senha do keystore' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:FLUXA_VERIFY_PASS = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $ErrorActionPreference = 'Continue'
    $out = & $KeytoolPath '-J-Duser.language=en' -list -v -keystore $Keystore -storetype PKCS12 '-storepass:env' FLUXA_VERIFY_PASS 2>$null | Out-String
    $ErrorActionPreference = 'Stop'
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    Remove-Item Env:FLUXA_VERIFY_PASS -ErrorAction SilentlyContinue
}
if ($out -match [regex]::Escape("SHA256: $expected")) { Write-Host 'OK: a cópia abre com a senha e tem o certificado do Fluxa.' -ForegroundColor Green }
else { throw 'FALHA: senha errada, arquivo corrompido ou certificado diferente.' }
