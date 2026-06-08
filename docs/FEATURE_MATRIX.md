# Матрица функций

| Функция | Статус | Примечание |
|---|---:|---|
| Client-only перехват | Готово | Только `ChatHud` / `ChatScreen` mixins |
| Сохранение Text click/hover | Готово | Работают серверные компоненты |
| Прозрачность, размеры, scale | Готово | GUI для основных, JSON для всех |
| Slide / fade | Готово | `scale` имеет fade fallback |
| Аватарки | Готово | Vanilla skin cache |
| Online-индикатор | Готово | Только наличие в player list |
| AFK/menu между модами | Не реализуется | Конфликт с запретом custom packets |
| Группировка | Готово | Окно задаётся в секундах |
| Временные метки | Готово | Java time pattern |
| ПКМ-меню | Готово | `/msg`, профиль, копирование, API |
| Party/guild | Через API | Нужен конкретный party-мод |
| Автоссылки | Готово | HTTP(S), без фонового title fetch |
| Серверный show_item | Готово | Исходный HoverEvent сохраняется |
| Локальный literal `[item]` | Ограничено | Нельзя восстановить предмет без данных |
| Unicode / named / ASCII emoji | Готово | Display и outgoing string |
| Custom PNG inline | Каркас | Сканирование есть, atlas отсутствует |
| Настройки с preview | Готово | Основные настройки в GUI |
| Drag & drop позиция | Через JSON | `position=CUSTOM`, `customX/customY` |
| Темы | Готово | 3 встроенные + JSON |
| Blacklist / regex | Готово | Client-side |
| Anti-spam | Готово | Последовательные дубликаты |
| Chatlog 10k | Готово | Асинхронная запись и pruning |
| Ctrl+F | Готово | Память + файлы |
| Упоминания | Готово | Подсветка и ванильный звук |
| Пользовательский `.ogg` | Каркас | Поля конфига есть, runtime resource loader отсутствует |
| Быстрый ответ | Готово | F4, обычная vanilla send path |
| LibreTranslate | Backend готов | Endpoint задаётся вручную, UI-пункт не добавлен |
| Forge / NeoForge | Нет | Эта ветка Fabric 1.21.11 |
