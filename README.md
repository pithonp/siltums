# Akumulators — Android lietotne + logrīks

- **Lietotne:** atver ESP tīmekļa lapu pilnekrānā (izvēlnē: Atjaunot, Adrese)
- **Logrīks 4×2:** atlikušais siltums (kWh, %), "Pietiks ~X" vai "▲ Kurina X kW · pilns ~hh:mm",
  temperatūras (augša/vidus/apakša, grīdas, āra). Fons oranžs = kurina, sarkans = brīdinājums.
  Pieskāriens atver lietotni, ↻ atjauno uzreiz. Fonā atjaunojas ik ~15 min.
- **Paziņojumi:** "Jākurina!" un "Grīdas par karstu!" (tikai, kad stāvoklis parādās)

## Uzbūvēt APK — variants A: GitHub (bez Android Studio)
1. Izveido jaunu GitHub repozitoriju un augšupielādē visu šīs mapes saturu (arī `.github`).
2. Cilnē **Actions** palaižas "Build APK" (~5 min).
3. Atver pabeigto darbu → **Artifacts → akumulators-apk** → lejupielādē ZIP, iekšā `app-debug.apk`.
4. Telefonā atver APK, atļauj "Instalēt no nezināmiem avotiem".

## Variants B: Android Studio
File → Open → šī mape → Run (vai Build → Build APK).

## Pirmā palaišana
Ievadi IP adresi, kas redzama ESP lapas augšā (piem. `192.168.0.186`).
Ieteicams maršrutētājā piešķirt ESP fiksētu IP (DHCP rezervācija), lai adrese nemainās.

Logrīks strādā tikai mājas WiFi tīklā. Ārpus mājām vajag VPN uz mājas tīklu (piem. WireGuard maršrutētājā vai Tailscale).
