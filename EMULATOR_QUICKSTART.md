# Wear-App im Emulator testen (3 Schritte)

## Wichtig

**Die Uhrzeit auf dem runden Display = System-Zifferblatt, nicht unsere App.**

Die App heißt **Eazpire Creator** und hat dunklen Hintergrund + orange Tabs oder ein Logo beim Start.

---

## Schritt 1: Projekt öffnen

In Android Studio **diesen Ordner** öffnen (nicht nur `android`):

```
…/eazpire/creator-watch
```

Modul oben: **`app`** auswählen.

---

## Schritt 2: Emulator starten & App installieren

1. **Tools → Device Manager** → **Wear OS XL Round** (oder **Wear OS Large Round**) → ▶ Start  
   - **Nicht** auf einem Phone-Emulator (Pixel) starten — die Wear-App braucht `android.hardware.type.watch`.
2. Run-Konfiguration: Modul **`app`**, Gerät = **Wear-Emulator** (nicht `android/` Phone-Projekt).
3. Grüner **Run**-Button (▶) klicken  
4. Unten: **Install successfully finished**

### App startet und beendet sich sofort?

| Ursache | Lösung |
|--------|--------|
| Phone-Emulator statt Wear | Wear OS Round Emulator verwenden (siehe oben). |
| Splash-Crash (API 31+) | Adaptive `@mipmap/ic_launcher` im System-Splash crasht — Fix: `@drawable/ic_wear_splash` in `values-v31/themes.xml`. **Rebuild** nach Pull. |
| Logcat prüfen | Android Studio → Logcat → Filter `com.eazpire.creator.wear`, Level **Error** → Zeile mit `FATAL EXCEPTION`. |

```text
adb logcat -s AndroidRuntime:E | findstr /i eazpire
```

---

## Schritt 3: App **auf der Uhr** öffnen (oft vergessen!)

Nach Run zeigt der Emulator oft noch das **Zifferblatt**. Dann:

1. **In das runde Emulator-Fenster klicken** (rechts in Studio)  
2. In der Leiste **über** der Uhr: Icon **App-Drawer** (Kreis mit 6 Punkten)  
   - oder vom Zifferblatt **nach oben wischen** (Maus: klicken + nach oben ziehen)  
3. **Eazpire Creator** antippen

### Erwartung

| Phase | Was du siehst |
|--------|----------------|
| Start | **Eazpire-Creator-Logo** zentriert (~2 s), dunkler Hintergrund |
| Danach | 3 Chips: **Dashboard** · **Active Jobs** · **Phone upload** (Demo-Daten im Emulator) |

---

## Terminal-Shortcut (Windows)

```powershell
cd "C:\Users\tobim\OneDrive\Dokumente\Cursor Projects\creator-worker"
npm run creator-watch:run-emulator
```

Startet die App direkt auf dem laufenden Emulator.

---

## Build-Fehler `packageDebug` / NullPointerException

Ursache war oft **OneDrive-Sperre** auf `creator-watch/*/build` oder kaputte Zwischenstände.

**Fix im Projekt:** Builds landen lokal unter  
`%LOCALAPPDATA%\eazpire-creator-watch-build\` (nicht im OneDrive-Ordner).

Wenn es in Android Studio noch scheitert:

1. **Build → Clean Project**, dann **Rebuild**
2. Oder Terminal: `cd creator-watch` → `.\gradlew.bat --stop` → `.\gradlew.bat :app:assembleDebug`
3. Bei „Unable to delete directory“: Android Studio kurz schließen, Befehl erneut ausführen

Debug-APK: `%LOCALAPPDATA%\eazpire-creator-watch-build\app\outputs\apk\debug\app-debug.apk`

## Nur ein kleiner Streifen oben („Eazpire Creator“) + Uhr?

**Wear OS XL API 36:** Die App braucht `targetSdk 36` + Wear-`Scaffold` + `AmbientLifecycleObserver`. Sonst zeigt das System nur einen **Streifen über dem Zifferblatt** (kein Widget).

**Nach Update unbedingt:**

```powershell
adb uninstall com.eazpire.creator.wear
cd creator-watch
.\gradlew.bat :app:installDebug
adb shell am start -n com.eazpire.creator.wear/.MainActivity
```

**Erwartung:** Logo → Tabs (Dashboard | Jobs | Upload) auf **vollem** runden Display.

**Falls nur Zifferblatt:** App-Drawer → **Eazpire Creator** (nicht nur die Uhr ansehen). Emulator: **Stay awake** in Entwickleroptionen.

**Fix:** App antippen / Bildschirm antippen → **interaktiver Modus** → volles Display (Logo, Tabs, Inhalt).

Im Emulator: **Einstellungen → Entwickleroptionen → Bildschirm bleibt aktiv** (Stay awake) oder Uhr während des Tests laden.

---

## Echte Daten (optional)

- Phone-Projekt: `creator-worker/android` → einloggen  
- Wear + Phone koppeln (echtes Gerät einfacher als 2 Emulatoren)
