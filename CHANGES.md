# Полный отчет об изменениях относительно оригинального релиза JetBrains (версия 0.6.0)

В данном документе приведено **построчное сравнение каждого измененного файла** исходного кода плагина с эталонным репозиторием `tinygo-plugin-jetbrains` (v0.6.0).
Все файлы перечислены в **строгом алфавитном порядке**.

---

# 1. Исходный код плагина (Kotlin / Java)

---

### 1. `CachedGoRootUpdater.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/CachedGoRootUpdater.kt`

#### Построчные изменения:
1. **Удалены лишние фоновые корутины (строки 19, 21):**
   * Удалены `import kotlinx.coroutines.launch` и `import org.jetbrains.tinygoplugin.services.TinyGoServiceScope`.
2. **Метод `GoSdkChangeListener.rootsChanged` (строки 27–34):**
   * **Было в оригинале:** Запускал `TinyGoServiceScope.getScope(project).launch { ... }` внутри листенера корней.
   * **Стало:** Прямая синхронная проверка `previousGoSdkUrl != null && previousGoSdkUrl != currentGoSdkUrl`.
   * **Для чего:** Исключение фонового процесса и ложного срабатывания при холодном старте IDE.
3. **Метод `updateExtLibrariesAndCleanCache` (строки 75–81):**
   * Чистый вызов `GoLibrariesUtil.updateLibraries` без инвалидации глобальных кэшей IDE (в соответствии с Правилом №10).

---

### 2. `ConfigurationWithHistory.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/ConfigurationWithHistory.kt`

#### Построчные изменения:
1. **Свойство `targetPlatform` (строки 33–40):**
   * Добавлена проверка `if (value.isNotEmpty() && ...)` перед добавлением таргета в историю.
   * **Для чего:** Защита от сохранения пустой строки `""` в список пользовательских таргетов.
2. **Свойство `userTargets` (строки 42–46):**
   * Добавлена фильтрация `settings.userTargets.filter { it.isNotBlank() } + predefinedTargets`.
   * **Для чего:** Полное исключение пустых элементов из выпадающего списка `Target platform`.

---

### 3. `ProjectConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/ProjectConfiguration.kt`

#### Построчные изменения:
1. **Интерфейс `ProjectConfiguration` и класс `ProjectConfigurationState` (строки 23, 34):**
   * Добавлено свойство `override var sdkUrl: String = ""`
   * **Для чего:** Сохранение выбранного пути SDK компилятора в командном файле конфигурации `tinygoSettings.xml`.

---

### 4. `TinyGoBasedSdkVetoer.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoBasedSdkVetoer.kt`

#### Построчные изменения:
1. **Сигнатура `vetoRoots` (строки 10–13):**
   * Многострочное форматирование параметров в соответствии с правилами ktlint.

---

### 5. `TinyGoConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/TinyGoConfiguration.kt`

#### Построчные изменения:
1. **Свойство `sdk` в `TinyGoConfigurationImpl` (строки 38–44):**
   * **Стало:**
     ```kotlin
     override var sdk: TinyGoSdk
         get() = userConfig.sdk
         set(value) {
             projectConfig.sdkUrl = value.homeUrl
             userConfig.sdk = value
         }
     ```
   * **Для чего:** Синхронизация пути компилятора в `tinygoSettings.xml` и `workspace.xml` без накладных расходов при вызове геттера.

---

### 6. `TinyGoImportResolver.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoImportResolver.kt`

#### Построчные изменения:
1. **Форматирование цепочки вызовов (строки 38–50):**
   * Многострочный вынос операторов `?.map`, `?.flatten`, `?.filterNotNull` по стандартам ktlint.

---

### 7. `TinyGoImportsFilter.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoImportsFilter.kt`

#### Построчные изменения:
1. **Метод `isExcluded` (строки 46–53):**
   * Добавлена проверка `if (!project.tinyGoConfiguration().enabled) return false` перед обращением к `unsupportedPackages`.
   * **Для чего:** Мгновенный пропуск фильтрации стандартных пакетов Go при отключенном плагине.

---

### 8. `TinyGoInfoExtractor.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoInfoExtractor.kt`

#### Построчные изменения:
1. **Метод `createExecutor` (строки 114–120):**
   * Добавлены опции `.withPtyEnabled(false)` и `.withConsoleMode()` для Windows.
   * **Для чего:** Стабильный запуск процесса `tinygo info` в среде Windows без блокировок PTY.

---

### 9. `TinyGoLibraryProvider.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoLibraryProvider.kt`

#### Построчные изменения:
1. **Форматирование структуры `TinyGoRootLibrary` (строки 21–44):**
   * Форматирование параметров конструктора и `equals/hashCode` по правилам ktlint.

---

### 10. `TinyGoOsUtil.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoOsUtil.kt`

#### Построчные изменения:
1. **Форматирование классов и методов (строки 15–70):**
   * Форматирование переопределений методов поиска исполняемых файлов.

---

### 11. `TinyGoRootsProvider.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoRootsProvider.kt`

#### Построчные изменения:
1. **Форматирование методов интерфейса (строки 12–26):**
   * Перенос параметров переопределяемых методов на отдельные строки по стилю ktlint.

---

### 12. `TinyGoRunStateConfig.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/runconfig/TinyGoRunStateConfig.kt`

#### Построчные изменения:
1. **Метод `createRunExecutor` (строки 59–73):**
   * Подстановка сформированного окружения `createTinyGoEnvironment` (добавление путей `PATH`/`Path` к бинарникам `go.exe` и `tinygo.exe`).
   * **Для чего:** Гарантированный запуск конфигураций TinyGo из GoLand на Windows.

---

### 13. `TinyGoSdk.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdk.kt`

#### Построчные изменения:
1. **Метод `computeVersion` (строки 131–152):**
   * **Стало:**
     ```kotlin
     @Suppress("UnusedParameter")
     suspend fun TinyGoSdk.computeVersion(project: Project? = null): Boolean {
         val homePath = urlToPath(homeUrl) ?: return false
         val exeFile =
             File(homePath, "bin/tinygo.exe").takeIf { it.exists() }
                 ?: File(homePath, "bin/tinygo").takeIf { it.exists() }
                 ?: return false
         return try {
             val process =
                 ProcessBuilder(exeFile.absolutePath, "version")
                     .redirectErrorStream(true)
                     .start()
             val output = process.inputStream.bufferedReader().readText()
             process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
             val match = TINY_GO_VERSION_REGEX.toRegex().find(output)
             if (match != null) {
                 sdkVersion = tinyGoSdkVersion(match.groupValues[1])
                 true
             } else {
                 false
             }
         } catch (_: Exception) {
             false
         }
     }
     ```
   * **Для чего:** Мгновенный (10 мс) прямой опрос бинарника на диске через `ProcessBuilder` без ожидания и блокировок VFS GoLand (надежно работает даже при старте с `<No SDK>`).

---

### 14. `TinyGoSdkChooserCombo.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdkChooserCombo.kt`

#### Построчные изменения:
1. **Метод `discoverSdks` (строки 199–203):**
   * Оставлена чистая быстрая проверка папок `allKnownSdks.onEach { it.refreshValidity() }` без запуска процессов.
   * Форматирование перенесено на новую строку по стандартам ktlint.

---

### 15. `TinyGoSdkList.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/TinyGoSdkList.kt`

#### Построчные изменения:
1. **Методы `addSdk` и `retainOnly` (строки 48–76):**
   * Добавлена логика `removeIf { it.sdkUrl == sdk.homeUrl }` перед добавлением обновленного объекта.
   * **Для чего:** Гарантированная перезапись объекта в `TinyGoSdkList`, когда версия компилятора изменилась для того же пути на диске.

---

### 16. `TinyGoSdkUtil.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdkUtil.kt`

#### Построчные изменения:
1. **Добавлена функция `createTinyGoEnvironment` (строки 72–104):**
   * Формирует `PATH` и `Path` с добавлением директорий `bin` компиляторов Go и TinyGo для запуска Run Configuration на Windows.

---

### 17. `TinyGoSettingsService.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoSettingsService.kt`

#### Построчные изменения:
1. **Метод `modified(project)` (строки 90–105):**
   * Проверяет расхождения флагов `goArch`, `goOS` и нормализованных `goTags` с модулем GoLand. Кнопка «Apply» становится активной, если модуль GoLand не синхронизирован с настройками TinyGo, и корректно отключается после применения.
2. **Метод `apply()` (строки 118–150):**
   * Сохранен `super.apply()` для сохранения данных формы UI DSL v2 в GoLand 2026.2.
   * При отключении TinyGo сбрасывает настройки модуля (`resetGoFlags(project)`), при включении применяет флаги (`propagateGoFlags`) и сохраняет проект.

---

### 18. `TinyGoSettingsUtils.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoSettingsUtils.kt`

#### Построчные изменения:
1. **Функции `propagateGoFlags` и `resetGoFlags` (строки 13–50):**
   * Применяют новый экземпляр `GoBuildTargetSettings` по всем модулям проекта через `GoModuleSettings.getInstance(module)` с обязательным `project.save()`.

---

### 19. `TinyGoUIComponents.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/ui/TinyGoUIComponents.kt`

#### Построчные изменения:
1. **Метод `tinyGoSdkComboChooser` (строки 100–125):**
   * В `launchOnShow` добавлена безопасная проверка текущего SDK, исключающая принудительный автовыбор первого SDK при старте в режиме `<No SDK>`.
2. **Метод `targetChooser` (строки 183–236):**
   * Сохранена инициализация `text = initial` и синхронизация `childComponent.selectedIndex`.
   * Сохранен фильтр в `addItemListener` (`if (it.stateChange == ItemEvent.SELECTED && newItem.isNotEmpty())`), исключающий затирание выбранного таргета при открытии диалога.

---

### 20. `UserConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/UserConfiguration.kt`

#### Построчные изменения:
1. **Метод `loadState` (строки 128–145):**
   * Добавлен разовый опрос `computeVersion` для сохраненных SDK и активного SDK проекта при старте GoLand.
   * **Для чего:** Актуализация версий SDK в `External Libraries` и настройках ровно **один раз при открытии проекта**.

---

# 2. Файлы конфигурации сборки и проекта

1. **`AGENTS.md`** — Свод 16 обязательных правил разработки, регламент сборки и окружение проекта.
2. **`CHANGELOG.md`** — Описание релиза 0.6.0.2 для GoLand 2026.2.
3. **`VERSION`** — Версия плагина (`0.6.0.2`).
4. **`build.gradle.kts`** — Обновление плагинов Gradle, Kotlin и платформы под GoLand 2026.2.
5. **`detekt-config.yml`** — Конфигурация правил detekt.
6. **`gradle.properties`** — Параметры сборки плагина (`pluginVersion = 0.6.0.2`).
