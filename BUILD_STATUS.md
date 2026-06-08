# Build status

## Проверено в этой поставке

- структура Fabric-проекта и обязательные metadata-файлы;
- корректность всех JSON-ресурсов;
- PNG-иконка;
- синтаксический проход Java 21: `javac` не обнаружил синтаксических ошибок до этапа разрешения внешних Minecraft/Fabric-классов;
- сигнатуры ключевых Minecraft 1.21.11 точек (`ChatHud`, `ChatScreen`, `KeyBinding`, `PlayerSkinDrawer`) сверены с Yarn `1.21.11+build.4`;
- отсутствие кода регистрации custom payload/network channel подтверждено поиском по исходникам.

## Почему JAR не вложен

Команда `./gradlew --no-daemon build` была запущена, но среда создания архива не смогла разрешить DNS-имя `services.gradle.org`. Сбой произошёл до загрузки Gradle и до компиляции проекта. Поэтому в архив намеренно не помещён неподтверждённый бинарник.

В обычной среде с доступом к Maven/Gradle сборка выполняется командой:

```bash
./gradlew --no-daemon build
```

Также приложен GitHub Actions workflow, который собирает JAR на Java 21 и публикует его как artifact.
