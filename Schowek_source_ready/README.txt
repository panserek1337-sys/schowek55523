Schowek — plugin (źródła) dla Paper 1.8.8
=============================================

Co jest w paczce:
- źródła Java: src/main/java/com/schowek/SchowekPlugin.java
- plugin.yml
- config.yml (polski, prosty)
- pom.xml (Maven)
- .github/workflows/build.yml (GitHub Actions - automatyczny build)
- README.txt z instrukcją jak używać GitHub Actions i jak zmieniać config

Szybkie instrukcje (GitHub Actions):
1. Utwórz nowe repo na GitHub.
2. Wgraj całą zawartość tej paczki do repo (wszystkie pliki i foldery).
3. Przejdź do zakładki Actions -> wybierz workflow "Build Schowek (Spigot 1.8.8)".
4. Kliknij "Run workflow" (jeśli nie ma, wykonaj commit, wtedy workflow uruchomi się automatycznie).
5. Po zakończeniu workflow wejdź w dane uruchomienie -> Artifacts -> pobierz "Schowek-jar" (to jest gotowy JAR).

Jak zmienić limity / ilości / dodać nowe przedmioty:
- Otwórz `config.yml` i w sekcji `przedmioty` dodaj nowy klucz:
  przyklad:
    diament:
      material: DIAMOND
      nazwa: "&bDiamenty"
      limit: 3
      ilosc: 4
- Możesz też ręcznie zmienić domyślne sekcje `limity` i `ilosci`, ale najlepiej edytować pod `przedmioty` każdy wpis.
- Po edycji użyj `/schowek reload` (wymaga permisji `schowek.admin`) lub zrestartuj serwer.

Jak zainstalować gotowy JAR (po wygenerowaniu):
1. Pobierz Schowek.jar z GitHub Actions lub zbuduj lokalnie.
2. Wrzuć do folderu plugins/ na serwerze Paper 1.8.8 przez FTP.
3. Uruchom lub zrestartuj serwer. Plugin utworzy config.yml jeśli nie istnieje.
4. Ustaw permisje (domyślnie schowek.uzyj = true dla wszystkich, schowek.admin = op).

Informacja o "Dobierz wszystko":
- Przyciski dają po jednym refillu (czyli jedna użycie zmniejsza limit o 1).
- Przycisk "Dobierz wszystko" spróbuje dodać każdemu brakujące przedmioty jednorazowo (po ilosc dla danego wpisu), tylko jeśli gracz ma wystarczająco dużo wolnych slotów w ekwipunku; w przeciwnym razie operacja się nie wykona.
- Komunikaty są bez prefixu i są kolorowane.