import hashlib,json,re,struct,zipfile
from pathlib import Path
apk=Path('artifacts/v1.0.1/Fluxa-1.0.1-s23.apk')
classes=set()
def u32(b,off): return struct.unpack_from('<I',b,off)[0]
with zipfile.ZipFile(apk) as z:
    names=z.namelist()
    forbidden=[n for n in names if re.search(r'\.(fluxa|db|sqlite|sqlite3|jks|keystore|p12|pfx|pem|key|md|log)$',n,re.I) or re.search(r'(^|/)(screenshots?|handoffs?|logcats?|dumps?|androidTest|test)(/|$)',n,re.I)]
    for name in names:
        if not re.fullmatch(r'classes\d*\.dex',name): continue
        b=z.read(name)
        string_off=u32(b,0x3c);type_off=u32(b,0x44)
        for i in range(u32(b,0x60)):
            idx=u32(b,u32(b,0x64)+i*32)
            si=u32(b,type_off+idx*4)
            pos=u32(b,string_off+si*4)
            while b[pos]&128: pos+=1
            pos+=1
            end=b.index(0,pos)
            classes.add(b[pos:end].decode('utf-8','replace'))
    unwanted=[c for c in classes if c.startswith(('Lcom/fluxa/personal/rc/','Lorg/junit/','Landroidx/compose/ui/tooling/')) or any(k in c for k in ('SecurityGateTestHook','BackupPickerTestHook','FluxaCatalogPreviews','VisualCapture','VisualSeed','FlowTest','ForceStopTest','RcFinancialSmokeTest','RcProcessDeathTest','Rc2Evidence','Rc2Upgrade','FinalUpgrade','FinalEvidence'))]
    required=['Lcom/fluxa/personal/security/LocalSecurityManager;','Landroidx/biometric/BiometricPrompt;']
    missing=[c for c in required if c not in classes]
    fixture_literal=any(b'FluxaRC2qa2026' in z.read(n) or b'androiddebugkey' in z.read(n) or b'-----BEGIN PRIVATE KEY-----' in z.read(n) for n in names if not n.endswith('/'))
    report={
        'filename':apk.name,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
        'entryCount':len(names),'classDefinitionCount':len(classes),
        'forbiddenFiles':forbidden,'unwantedClasses':unwanted,'missingSecurityClasses':missing,
        'knownQaSecretOrPrivateKeyLiteralPackaged':fixture_literal,
        'nativeLibraries':[n for n in names if n.endswith('.so')],
        'assets':[n for n in names if n.startswith('assets/')],
        'runtime16KBTested':False,
    }
    Path('artifacts/v1.0.1/apk-entry-list.txt').write_text('\n'.join(names),encoding='utf-8')
    Path('artifacts/v1.0.1/apk-content-audit.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2))
    assert not forbidden and not unwanted and not missing and not fixture_literal
