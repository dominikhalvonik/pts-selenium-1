# TestLab Shop – Cvičenie 02: Selenium a JUnit

Hotové riešenie praktickej časti prezentácie **02_Selenium_Java_Web_GUI.pptx**.
Projekt má **jeden JUnit GUI test**, ktorý vytvorí referenčnú objednávku, overí
výsledok a uloží dôkazy. Ide o Maven testovací projekt, nie o ďalšiu webovú aplikáciu
ani program s metódou `main()`.

## Čo test vykonáva

1. Otvorí nový prehliadač s dočasným profilom, prihlási účet pracovného priestoru
   a overí prázdny košík a dostupné zásoby.
2. Cez GUI pridá **KB-001 × 2** a **MS-001 × 1**, vyberie kuriéra a kupón **STUDENT10**.
3. Vyplní simulované kontaktné údaje a odošle objednávku **iba raz**.
4. Z detailu uloží UUID ešte pred kontrolou ceny. Overí **8563 centov**,
   **85,63 €** po normalizácii medzier, **created**, identitu a množstvá.
5. Pri chybe uloží screenshot a výnimku **pred** uprataním a zatvorením prehliadača.
6. Predvolene zruší iba svoju objednávku cez GUI a zatvorí prehliadač.


## 1. Predpoklady

- **JDK 17 alebo 21** (celé JDK, nie iba JRE).
- **Apache Maven 3.9.x** na PATH, alebo Maven dostupný priamo v IDE.
- Nainštalovaný **Google Chrome**; voliteľne Microsoft Edge alebo Firefox.
- Dostupná pôvodná aplikácia **TestLab Shop 1.0.0** s pripravenou MariaDB.
- Internet pri prvom stiahnutí Maven závislostí a ovládača prehliadača.

Projekt fixuje Selenium **4.49.0**, JUnit Jupiter **5.12.2**, Maven Compiler Plugin
**3.14.0** a Maven Surefire Plugin **3.5.3**. JUnit 5 je zvolená verzia projektu,
nie tvrdenie, že je najnovšia. Na výučbu netreba Selenium Grid ani samostatný
Selenium Server. Súčasťou nie je Maven Wrapper, príkazy sú `mvn`, nie `mvnw`.

## 2. Spustenie webu a vytvorenie dát

Tieto príkazy spúšťajte v **pôvodnom PHP projekte**, NIE v tomto Maven projekte.
Ak je web už nasadený na hostingu, použite jeho existujúcu adresu a účet.

```powershell
cd C:\projekty\testlab-shop
php bin/check.php
php bin/fixtures.php create
```

`fixtures.php create` potrebuje funkčnú DB a režim `app_env=local` alebo `testing`.
Vráti objekt s poľami **workspace_code**, **email** a **password** (priamo na
najvyššej úrovni JSON, nie pod `data` alebo `user`).

Po inicializácii aplikácie spustite PHP v osobitnom termináli:

```powershell
php -S 127.0.0.1:8080 -t public
```

Na počítači s testom musí fungovať adresa webu:

```text
http://127.0.0.1:8080/
```

Selenium tu beží v desktopovom prehliadači, NIE v Android emulátore. Preto pre
server na tom istom PC používame `127.0.0.1`, nie emulátorovú adresu `10.0.2.2`.

Testovací projekt nemení backend, nevytvára tabuľky, nezapína chybové režimy a
nepotrebuje databázové heslo ani `X-Test-Key`. Normálny beh predpokladá
`fault_mode=none`.

## 3. Konfigurácia testu

V tomto Maven projekte:

```powershell
Copy-Item config\test.example.properties config\test.properties
```

Upravte `config/test.properties`:

```properties
base.url=http://127.0.0.1:8080/
test.email=SEM_SKUTOCNY_EMAIL_Z_FIXTURE
test.password=SEM_SKUTOCNE_HESLO_Z_FIXTURE
browser=chrome
headless=false
timeout.seconds=15
cleanup.order=true
app.version=1.0.0
```

Hodnoty začínajúce `SEM_` sú zástupné a zámerne sa odmietnu. Nahraďte ich
skutočným výstupom prípravy dát. `app.version` je deklarácia nasadenej verzie od
vyučujúceho. Test ju automaticky neoveruje a nečíta cez API.

Zadávajte **adresu webu**, nie `/api/index.php`. Podpriečinok funguje napríklad ako
`https://example.test/testlab/`. Nepoužívajte URL s prihlasovacími údajmi, query
parametrami ani hash fragmentom.

Pri prvom lokálnom predvedení bez izolácie môžete explicitne použiť pôvodný demo
účet `student@example.test` / `Student123!`. Ten však zdieľa sklad s druhým demo
účtom a **nenahrádza požiadavku prezentácie na izolovaný účet**. Nepoužívajte ho
pre súbežné skupiny ani pri verejnom nasadení s predvoleným heslom.

### Alternatíva: premenné prostredia

```powershell
$env:BASE_URL = 'http://127.0.0.1:8080/'
$env:TEST_EMAIL = 'EMAIL_PRIDELENY_VYUCUJUCIM'
$env:TEST_PASSWORD = 'HESLO_PRIDELENE_VYUCUJUCIM'
mvn test
```

Konfiguračná priorita je **JVM `-D` > prostredie > `config/test.properties` > default**.
Heslo radšej neposielajte cez príkazový riadok; môže sa uložiť do histórie.

| Parameter | Premenná prostredia | Default |
|---|---|---|
| `base.url` | `BASE_URL` | `http://127.0.0.1:8080/` |
| `test.email` | `TEST_EMAIL` | povinný |
| `test.password` | `TEST_PASSWORD` | povinný |
| `browser` | `BROWSER` | `chrome` |
| `headless` | `HEADLESS` | `false` |
| `timeout.seconds` | `TIMEOUT_SECONDS` | `15`, povolené 1–120 |
| `cleanup.order` | `CLEANUP_ORDER` | `true` |
| `app.version` | `APP_VERSION` | `NEZADANÁ` |
| `driver.path` | `DRIVER_PATH` | automatický Selenium Manager |
| `browser.binary` | `BROWSER_BINARY` | štandardná inštalácia prehliadača |

`config/test.properties` je v `.gitignore`. Konfiguračný súbor sa číta ako UTF-8.
V cestách na Windows používajte `/` alebo escapované `\\`.

## 4. Spustenie z IntelliJ IDEA / iného IDE

Otvorte koreňový priečinok projektu alebo jeho **pom.xml** ako Maven projekt.
Nastavte Project SDK 17 alebo 21 a nechajte IDE stiahnuť závislosti.

Otvorte:

```text
src/test/java/sk/testlab/cvicenie02/ReferenceOrderTest.java
```

Spustite zelenou šípkou metódu `referenceOrder()` alebo celú triedu. Pracovný
priečinok konfigurácie Run musí byť koreň projektu (obsahujúci `pom.xml` a `config`).
Netreba vytvárať `main()` ani Java Application run konfiguráciu: je to **JUnit**.
Alternatívne v Maven okne IDE spustite cieľ `test`.


## 5. Výstupy a upratanie

Každý beh vytvorí samostatný priečinok:

```text
target/artifacts/<UTC-cas-a-identifikator>/
    run.properties
    order-id.txt                     # keď sa podarilo získať UUID
    passed-before-cleanup.png         # úspešné assertions, ešte created
    failure.txt                      # pri chybe testu
    failure.png                      # pri chybe testu a dostupnom prehliadači
    cleanup-failure.txt/.png          # iba pri chybe upratania
```

`run.properties` obsahuje posledný krok, deklarovanú verziu aplikácie, skutočnú
verziu prehliadača, namerané hodnoty a výsledok upratania. Nie sú v ňom heslá,
bearer tokeny ani databázové údaje. UUID môže chýbať, keď objednávka nevznikla alebo
odpoveď neprišla. Screenshot nemožno získať pred vytvorením relácie alebo po páde
prehliadača; vtedy sa zaznamená jeho nedostupnosť a pôvodná chyba zostáva zachovaná.

Maven/JUnit reporty pri spustení cez Maven sú v **target/surefire-reports/**.
Pri spustení cez IDE je JUnit výsledok v jeho testovacom okne; vlastné artefakty
sa ukladajú rovnako. `mvn clean` odstráni `target`, teda aj staré dôkazy.

### Prečo je objednávka po úspešnom teste cancelled?

Assertions sa vykonajú nad stavom **created**. Až potom `@AfterEach` objednávku
zruší, aby server vrátil skladové zásoby. História sa nemaže. Nie je to ďalší
akceptačný scenár, ale upratovacia infraštruktúra. Samotný GUI test nepotvrdzuje
vnútornú správnosť skladovej transakcie; to patrí do testovania DB/backendu.

Na ručné pozretie vytvorenej objednávky ponechajte dáta:

```powershell
mvn test "-Dcleanup.order=false"
```

Vtedy sa zásoby znížia a objednávka zostane `created`. Zrušte ju ručne, alebo
vyučujúci neskôr odstráni presný izolovaný priestor v PHP projekte:

```powershell
php bin/fixtures.php delete run-KONKRETNY_KOD_Z_VYTVORENIA
```

Zástupný text nahraďte skutočným kódom. Mazanie priestoru je **nevratné** pre jeho
objednávky a účty; nerobte ho počas prebiehajúcich testov. Nikdy nemažte priestor
inej skupiny. Test tento príkaz sám nespúšťa.

Ak odoslanie prebehlo, ale UUID ostalo neznáme, test nič naslepo neopakuje ani
nehľadá „poslednú objednávku“ na zrušenie. Do dôkazov zapíše požiadavku na ručnú
kontrolu vlastného pracovného priestoru.