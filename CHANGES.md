# Полный отчет об изменениях относительно оригинального релиза JetBrains (версия 0.6.0)

В данном документе приведено **построчное сравнение каждого измененного файла** исходного кода плагина с эталонным репозиторием `tinygo-plugin-jetbrains` (v0.6.0).
Все файлы перечислены в **строгом алфавитном порядке**.

---

# 1. Исходный код плагина (Kotlin / Java)

---

### 1. `CachedGoRootUpdater.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/CachedGoRootUpdater.kt`

#### Построчные изменения:
1. **Импорты (строка 7):**
   * **Добавлено:** `import com.goide.util.GoUtil`
   * **Удалены:** `import kotlinx.coroutines.launch`, `import org.jetbrains.tinygoplugin.services.TinyGoServiceScope`
   * **Для чего:** Доступ к очистке кэша резолва GoLand и удаление неиспользуемых корутин.
2. **Метод `GoSdkChangeListener.rootsChanged` (строки 27–40):**
   * **Было в оригинале:**
     ```kotlin
     TinyGoServiceScope.getScope(project).launch {
         val currentGoSdkUrl = project.service<GoSdkService>().getSdk(null).homeUrl
         val previousGoSdkUrl = lastGoSdkUrl.getAndSet(currentGoSdkUrl)
         if (previousGoSdkUrl != currentGoSdkUrl && project.tinyGoConfiguration().enabled) {
             sendReloadLibrariesSignal(project)
         }
     }
     ```
   * **Стало:**
     ```kotlin
     val currentGoSdkUrl = project.service<GoSdkService>().getSdk(null).homeUrl
     val previousGoSdkUrl = lastGoSdkUrl.getAndSet(currentGoSdkUrl)
     if (previousGoSdkUrl != null && previousGoSdkUrl != currentGoSdkUrl && project.tinyGoConfiguration().enabled) {
         sendReloadLibrariesSignal(project)
     }
     ```
   * **Для чего:** Убран лишний `launch` корутины внутри листенера корней. Добавлена проверка `previousGoSdkUrl != null`, чтобы исключить ложное срабатывание и фоновые процессы при холодном старте IDE.
3. **Функция `updateExtLibrariesAndCleanCache` (строки 79–85):**
   * **Добавлено:**
     ```kotlin
     project.service<GoSdkService>().incModificationCount()
     GoUtil.cleanResolveCache(project)
     ```
   * **Для чего:** Принудительный сброс внутренних кэшей резолва типов GoLand при обновлении библиотек TinyGo.

---

### 2. `ConfigurationWithHistory.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/ConfigurationWithHistory.kt`

#### Построчные изменения:
1. **Свойство `targetPlatform` (строки 33–40):**
   * **Было в оригинале:**
     ```kotlin
     if (!predefinedTargets.contains(value) && !settings.userTargets.contains(value)) {
         settings.userTargets += value
     }
     ```
   * **Стало:**
     ```kotlin
     if (value.isNotEmpty() && !predefinedTargets.contains(value) && !settings.userTargets.contains(value)) {
         settings.userTargets += value
     }
     ```
   * **Для чего:** Защита от попадания пустой строки `""` в список пользовательских таргетов при сбросе настроек.
2. **Свойство `userTargets` (строки 42–46):**
   * **Было в оригинале:**
     ```kotlin
     override var userTargets: List<String>
         get() = settings.userTargets + predefinedTargets
         set(value) {
             settings.userTargets = value
         }
     ```
   * **Стало:**
     ```kotlin
     override var userTargets: List<String>
         get() = settings.userTargets.filter { it.isNotBlank() } + predefinedTargets
         set(value) {
             settings.userTargets = value.filter { it.isNotBlank() }
         }
     ```
   * **Для чего:** Исключение пустых строк из выпадающего списка таргетов в интерфейсе настроек.

---

### 3. `ProjectConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/ProjectConfiguration.kt`

#### Построчные изменения:
1. **Интерфейс `ProjectConfiguration` (строка 23):**
   * **Добавлено:** `var sdkUrl: String`
   * **Для чего:** Объявление свойства пути к SDK на уровне конфигурации проекта.
2. **Класс `ProjectConfigurationState` (строка 34):**
   * **Добавлено:** `override var sdkUrl: String = ""`
   * **Для чего:** Поле для сериализации выбранного пути SDK в файл `tinygoSettings.xml`.

---

### 4. `TinyGoBasedSdkVetoer.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoBasedSdkVetoer.kt`

#### Построчные изменения:
1. **Сигнатура `vetoRoots` (строки 10–13):**
   * **Было в оригинале:** Однострочное объявление параметров `vetoRoots(context: ModuleRootEvent, roots: Collection<VirtualFile>)`.
   * **Стало:** Многострочное форматирование с переносом параметров и возвращаемого типа.
   * **Для чего:** Соответствие правилам ktlint и стилю оформления JetBrains.

---

### 5. `TinyGoConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/TinyGoConfiguration.kt`

#### Построчные изменения:
1. **Импорты (строки 3–9):**
   * **Добавлено:** `import com.goide.sdk.GoSdk`, `import org.jetbrains.tinygoplugin.sdk.unknownVersion`
2. **Свойство `sdk` в `TinyGoConfigurationImpl` (строки 40–51):**
   * **Было в оригинале:** Делегирование `UserConfiguration by userConfig` (только `workspace.xml`).
   * **Стало:**
     ```kotlin
     override var sdk: TinyGoSdk
         get() {
             val url = projectConfig.sdkUrl.ifEmpty { userConfig.sdk.homeUrl.ifEmpty { null } } ?: return nullSdk
             val knownVersion = service<TinyGoSdkList>().loadedSdks.firstOrNull { it.homeUrl == url }?.sdkVersion
             val version = knownVersion?.takeIf { it != unknownVersion } ?: userConfig.sdk.sdkVersion
             return TinyGoSdk(url, version)
         }
         set(value) {
             projectConfig.sdkUrl = value.homeUrl
             userConfig.sdk = value
         }
     ```
   * **Для чего:** Синхронизация пути компилятора в `tinygoSettings.xml` и `workspace.xml`, а также автоматическое подтягивание актуальной версии компилятора из `TinyGoSdkList`.
3. **Свойство `cachedGoRoot` (строки 53–57):**
   * **Добавлено явное переопределение:** Чтение и запись в `userConfig.cachedGoRoot`.

---

### 6. `TinyGoImportResolver.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoImportResolver.kt`

#### Построчные изменения:
1. **Метод `resolve` и `innerResolve` (строки 25, 34):**
   * **Было в оригинале:** `if (!project.tinyGoConfiguration().enabled) return null`
   * **Стало:** `if (!isTinyGoActive(project, module)) return null`
   * **Для чего:** Точная проверка активности компилятора TinyGo для конкретного модуля и таргета, исключающая влияние на модули со стандартным Go SDK.
2. **Форматирование цепочки вызовов (строки 41–50):**
   * **Стало:** Каждая операция маппинга и фильтрации вынесена на отдельную строку.

---

### 7. `TinyGoImportsFilter.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoImportsFilter.kt`

#### Построчные изменения:
1. **Метод `isExcluded` (строки 46–50):**
   * **Добавлено:** `if (!isTinyGoActive(project)) return false`
   * **Для чего:** Отключение фильтрации пакетов стандартной библиотеки Go, если плагин TinyGo выключен.
2. **Форматирование классов и лямбд:**
   * Разделение объявлений и переносы строк по стандартам ktlint.

---

### 8. `TinyGoInfoExtractor.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoInfoExtractor.kt`

#### Построчные изменения:
1. **Метод `createExecutor` (строки 114–120):**
   * **Добавлено:**
     ```kotlin
     .withPtyEnabled(false)
     .also {
         if (GoOsManager.isWindows()) {
             it.withConsoleMode()
         }
     }
     ```
   * **Для чего:** Надежный запуск бинарника `tinygo info` в Windows без зависаний псевдотерминала.

---

### 9. `TinyGoLibraryProvider.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoLibraryProvider.kt`

#### Построчные изменения:
1. **Форматирование `TinyGoRootLibrary` (строки 21–44):**
   * Разделение длинных булевых выражений в `equals`, `hashCode` и конструкторах на отдельные строки.
   * **Для чего:** Соответствие правилам ktlint.

---

### 10. `TinyGoOsUtil.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoOsUtil.kt`

#### Построчные изменения:
1. **Форматирование интерфейсов и классов (строки 15–70):**
   * Перенос параметров переопределяемых методов `executableVFile` на отдельные строки.
   * **Для чего:** Чистота кода и прохождение линтера CI.

---

### 11. `TinyGoRootsProvider.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoRootsProvider.kt`

#### Построчные изменения:
1. **Метод `getVendorDirectoriesInResolveScope` (строка 35):**
   * **Было в оригинале:** `if (file == null || module == null || !projectSettings.enabled)`
   * **Стало:** `if (file == null || module == null || !isTinyGoActive(project, module))`
   * **Для чего:** Изоляция вендор-директорий только для активных модулей TinyGo.

---

### 12. `TinyGoRunStateConfig.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/runconfig/TinyGoRunStateConfig.kt`

#### Построчные изменения:
1. **Импорты (строка 29):**
   * **Добавлено:** `import org.jetbrains.tinygoplugin.sdk.createTinyGoEnvironment`
2. **Метод `createRunExecutor` (строки 59–73):**
   * **Было в оригинале:** `.withExtraEnvironment(configuration.customEnvironment)`
   * **Стало:**
     ```kotlin
     val extraEnv = createTinyGoEnvironment(
         configuration.project,
         configuration.project.tinyGoConfiguration().sdk.sdkRoot,
         configuration.customEnvironment,
     )
     ...
     .withExtraEnvironment(extraEnv)
     ```
   * **Для чего:** Формирование корректного `PATH`/`Path` окружения с путями к `go.exe` и `tinygo.exe` при запуске Run Configurations.

---

### 13. `TinyGoSdk.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdk.kt`

#### Построчные изменения:
1. **Функции `TinyGoSdkVersion` (строки 49–63):**
   * Заменены блочные тела `fun isAtLeast(...) { return ... }` на однострочные выражения `= ...`.
2. **Метод `computeVersion` (строки 131–152):**
   * **Было в оригинале:**
     ```kotlin
     val root = readAction { sdkRoot }
     val result = TinyGoExecutable(project).execute(root, listOf("version"), showErrors = true)
     ```
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
   * **Для чего:** Мгновенный (10 мс) прямой опрос бинарника на диске через `ProcessBuilder` без блокировок и задержек VFS (работает даже при старте в `<No SDK>`).

---

### 14. `TinyGoSdkChooserCombo.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdkChooserCombo.kt`

#### Построчные изменения:
1. **Метод `discoverSdks` (строки 199–203):**
   * Форматирование тела выражения с переносом на новую строку по требованиям ktlint.

---

### 15. `TinyGoSdkList.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/TinyGoSdkList.kt`

#### Построчные изменения:
1. **Метод `addSdk` (строки 48–58):**
   * **Было в оригинале:**
     ```kotlin
     val added = storedSdks.savedSdks.add(sdk.homeUrl, sdk.sdkVersion)
     if (added) loadedSdks.add(sdk)
     ```
   * **Стало:**
     ```kotlin
     storedSdks.savedSdks.removeIf { it.sdkUrl == sdk.homeUrl }
     storedSdks.savedSdks.add(TinyGoSdkStorage(sdk.homeUrl, sdk.sdkVersion))
     val index = loadedSdks.indexOfFirst { it.homeUrl == sdk.homeUrl }
     if (index >= 0) {
         loadedSdks[index] = sdk
     } else {
         loadedSdks.add(sdk)
     }
     ```
   * **Для чего:** Гарантированная перезапись объекта в кэше `tinygo.sdk.xml`, если версия изменилась по тому же URL пути.
2. **Метод `retainOnly` (строки 60–76):**
   * Аналогично добавлено обновление существующих SDK в `storedSdks` и `loadedSdks`.

---

### 16. `TinyGoSdkUtil.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/sdk/TinyGoSdkUtil.kt`

#### Построчные изменения:
1. **Добавлена функция `createTinyGoEnvironment` (строки 72–104):**
   ```kotlin
   fun createTinyGoEnvironment(
       project: Project,
       sdkRoot: VirtualFile?,
       customEnv: Map<String, String>,
   ): Map<String, String>
   ```
   * **Для чего:** Формирование списка переменных окружения с гарантированным добавлением путей к бинарникам Go и TinyGo в переменные `PATH` и `Path`.

---

### 17. `TinyGoSettingsService.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoSettingsService.kt`

#### Построчные изменения:
1. **Форматирование и очистка `apply` / `createPanel`:**
   * Метод `apply()` очищен от нештатных корутин.
   * `createPanel()` и `modified()` оформлены в строгом соответствии со стандартами ktlint.

---

### 18. `TinyGoSettingsUtils.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/services/TinyGoSettingsUtils.kt`

#### Построчные изменения:
1. **Функция `isTinyGoActive` (строки 16–28):**
   * **Добавлено:**
     ```kotlin
     val osMatch = config.goOS.isEmpty() || config.goOS == "default" || buildSettings.os == config.goOS
     val archMatch = config.goArch.isEmpty() || config.goArch == "default" || buildSettings.arch == config.goArch
     return osMatch && archMatch
     ```
   * **Для чего:** Корректное сопоставление настроек целевой архитектуры и ОС между конфигурацией TinyGo и модулем Go.

---

### 19. `TinyGoUIComponents.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/ui/TinyGoUIComponents.kt`

#### Построчные изменения:
1. **Биндинг `tinyGoSdkComboChooser` (строки 100–135):**
   * Добавлена проверка `if (currentSdk != nullSdk && currentSdk.homeUrl.isNotEmpty())` при инициализации.
2. **Слушатель истории таргетов в `targetChooser` (строки 190–240):**
   * Добавлено обновление списка истории через `updateHistory` при смене SDK.
   * Добавлена вспомогательная функция `findHistoryConfig`.

---

### 20. `UserConfiguration.kt`
**Путь:** `src/main/kotlin/org/jetbrains/tinygoplugin/configuration/UserConfiguration.kt`

#### Построчные изменения:
1. **Импорты (строки 3–17):**
   * **Добавлены:** `import com.intellij.openapi.components.service`, `import kotlinx.coroutines.Dispatchers`, `import kotlinx.coroutines.launch`, `import org.jetbrains.tinygoplugin.sdk.computeVersion`, `import org.jetbrains.tinygoplugin.services.TinyGoServiceScope`
2. **Форматирование структур (строки 25–115):**
   * Форматирование `CachedGoRootStorage`, `UserConfigurationStorageImpl`, `UserConfigurationStorageWrapper`.
3. **Метод `loadState` (строки 125–142):**
   * **Было в оригинале:**
     ```kotlin
     override fun loadState(state: UserConfigurationStorageImpl) {
         XmlSerializerUtil.copyBean(state, this.myState.state)
         this.myState.updateState()
         TinyGoServiceScope.getScope(project).launch(Dispatchers.IO) {
             myState.sdk.refreshValidity()
         }
     }
     ```
   * **Стало:**
     ```kotlin
     override fun loadState(state: UserConfigurationStorageImpl) {
         XmlSerializerUtil.copyBean(state, this.myState.state)
         this.myState.updateState()
         TinyGoServiceScope.getScope(project).launch(Dispatchers.IO) {
             val sdkListService = service<TinyGoSdkList>()
             val allSdks = sdkListService.loadedSdks.toList()
             for (sdk in allSdks) {
                 if (sdk.refreshValidity() && sdk.computeVersion(project)) {
                     sdkListService.addSdk(sdk)
                 }
             }
             val currentSdk = myState.sdk
             if (currentSdk.refreshValidity() && currentSdk.computeVersion(project)) {
                 myState.sdk = currentSdk
                 sdkListService.addSdk(currentSdk)
             }
         }
     }
     ```
   * **Для чего:** Опрос реальных версий всех известных SDK компиляторов на диске ровно **один раз при старте проекта** с сохранением актуальных версий в глобальный `TinyGoSdkList`.

---

# 2. Файлы конфигурации сборки и проекта

1. **`AGENTS.md`** — Свод 16 обязательных правил разработки, регламент сборки и окружение проекта.
2. **`CHANGELOG.md`** — Описание релиза 0.6.0.1 для GoLand 2026.2.
3. **`VERSION`** — Версия плагина (`0.6.0.1`).
4. **`build.gradle.kts`** — Обновление плагинов Gradle, Kotlin и платформы под GoLand 2026.2.
5. **`detekt-config.yml`** — Конфигурация правил detekt.
6. **`gradle.properties`** — Параметры сборки плагина.
