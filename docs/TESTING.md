# Testes

## Visão geral

| Suíte | Onde | Quantidade | Precisa de emulador |
| --- | --- | --- | --- |
| JVM | `android/app/src/test` | **326 testes** em 40 arquivos | Não |
| Instrumentada | `android/app/src/androidTest` | **200 casos** em 50 arquivos | Sim |
| Lint | — | debug e release | Não |

Números da versão 1.0.2. Cada teste é contado uma vez; execuções repetidas em perfis diferentes não são somadas.

## JVM

```bash
cd android
./gradlew testDebugUnitTest
```

Cobre:
- **Domínio**: `Money`, `BillingDay`, saldos, divisão de parcelas, calendário de cartão, pagamento de parcelas, assinaturas e desfazer, metas e reservas, Limite seguro e análises, incluindo bordas como fim de mês, estouro e eventos conflitantes.
- **Backup**: codec, criptografia (senha errada, arquivo truncado ou alterado, versões não suportadas, limites de tamanho) e cópia preventiva.
- **Segurança**: a máquina de estados do bloqueio, com relógio controlado.
- **Interface**: ViewModels com fontes falsas, formatação de valores e datas, contraste de cores (WCAG AA nos dois temas) e componentes do design system.

Relatório: `android/app/build/reports/tests/testDebugUnitTest/index.html`.

## Instrumentados

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.deviceCredential=<PIN do emulador>
```

Cobre:
- **Banco**: schema, persistência de cada área e migrations a partir de cada versão anterior.
- **Fluxos de interface**: contas e categorias, lançamentos, cartões e compras, pagamento de parcelas, assinaturas, metas, Limite seguro, Análises, aparência, backup e restauração pelo seletor real do Android, e segurança.
- **Contrato do pacote**: `applicationId`, versão, permissões, componentes exportados, ausência de classes de debug na release e ícone adaptável dentro da área segura.
- **Jornada financeira** completa, da receita às Análises, numa única sessão.

### Testes sob demanda

27 casos só rodam quando recebem um argumento, porque precisam de passos fora do teste, como encerrar o processo ou instalar outro APK. Sem o argumento, aparecem como *pulados*:

| Argumento | Para quê |
| --- | --- |
| `themePhase`, `analyticsPhase`, `subscriptionPhase`, `localSecurityPhase` | Estado preservado após `am force-stop` |
| `finalPhase`, `v101Phase`, `v102Phase` | Atualização entre versões sem perder dados |
| `fluxaCapture` | Gerar capturas de tela com dados fictícios |

### Matriz usada nas entregas

A suíte completa roda **sem rede** (modo avião) num emulador API 34, em cinco perfis:

| Perfil | 1.0.2 |
| --- | --- |
| 320×640 | 0 falhas |
| 360 | 0 falhas |
| 390, tema escuro | 0 falhas |
| 412 | 0 falhas |
| 320, fonte 130% | 0 falhas |

## Integração contínua

O workflow [`.github/workflows/android.yml`](../.github/workflows/android.yml) roda a cada push ou pull request que mexe em `android/`. Ele compila debug, release e o APK de testes, roda o lint e os testes JVM e confere se os schemas do Room estão versionados. Os testes instrumentados não rodam na CI: precisam de emulador e rodam localmente antes de cada versão.

## Testes de atualização

Para cada versão, o APK anterior é instalado, um conjunto representativo de dados é criado e o APK novo é instalado por cima (`adb install -r`), sem desinstalar. Depois, um teste confirma:

- `versionCode` novo e `firstInstallTime` preservado;
- todas as tabelas, preferências, configurações de segurança, Limite seguro e Análises idênticos aos de antes;
- que o app abre pedindo autenticação quando o bloqueio estava ligado.

## Limitações conhecidas

- O teste do prompt real de PIN do Android (`LocalSecurityActivityTest.credentialOnlyDevice…`) oscila ocasionalmente em telas de 320 dp. Ele passa ao ser repetido, e a causa não foi investigada.
- Os testes de biometria usam PIN. Leitores biométricos reais só são verificados em aparelho físico.
- Não há testes automatizados em aparelho físico. A validação física foi manual, no Galaxy S23 do desenvolvedor.
- O comportamento em aparelhos com páginas de memória de 16 KB não foi testado em execução. O alinhamento das bibliotecas nativas foi conferido no APK.
- Não há medição de cobertura de código.
