# API LabyChat Standalone

Публичные типы находятся в `io.github.labychat.api`.

## Трансформер сообщения

```java
LabyChatAPI.registerMessageTransformer((message, context) -> {
    if (message.getString().contains("локально скрыть")) {
        return Optional.empty();
    }
    return Optional.of(message.copy());
});
```

Трансформер получает display-копию. `Optional.empty()` скрывает сообщение только в кастомном рендере и не меняет серверное состояние.

`TransformContext` содержит клиент, время, распознанного отправителя и признак системного сообщения.

## Пункт контекстного меню

```java
LabyChatAPI.registerContextMenuEntry("example:copy_command", new IContextMenuEntry() {
    @Override
    public Text label(ContextMenuContext context) {
        return Text.literal("Copy /msg command");
    }

    @Override
    public boolean isVisible(ContextMenuContext context) {
        return context.client().getNetworkHandler() != null;
    }

    @Override
    public void activate(ContextMenuContext context) {
        context.client().keyboard.setClipboard("/msg " + context.playerName() + " ");
    }
});
```

Идентификаторы должны быть уникальными. Повторная регистрация заменяет запись с тем же id.

## Эмодзи-ресурс

```java
LabyChatAPI.addEmoji(
        ":example:",
        Identifier.of("example_mod", "textures/gui/emoji/example.png")
);
```

Текущая версия регистрирует ресурс для панели и будущего inline atlas. До реализации texture-glyph renderer shortcode вставляется текстом.

## Гарантии

API предназначен только для клиентских действий. Реализации не должны отправлять нестандартные пакеты от имени LabyChat Standalone и не должны выполнять тяжёлую работу в render thread.
