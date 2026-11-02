# АвтоВАЗ Мод (`avtovazmod`)

NeoForge **21.1.x** для Minecraft **1.21.1**, Java 21. Две машины, на которых можно ездить:
ВАЗ-2107 «Семёрка» и ВАЗ-2121 «Нива».

## Запуск

```
gradle wrapper --gradle-version 8.10.2   # один раз, если нет gradlew (или откройте проект в IntelliJ)
./gradlew runClient
./gradlew build                          # jar в build/libs
```
Если `neo_version` из `gradle.properties` не находится, поставьте любую актуальную 21.1.x.

## Управление
| Действие | Клавиша |
|---|---|
| Сесть | ПКМ по машине (до 4 человек: водитель + 3) |
| Газ / тормоз / задний ход | W / S |
| Руль | A / D |
| Ручник (занос) | Пробел |
| Выйти | Shift |
| Свернуть машину в предмет | Shift + ПКМ (пустая машина) или удары |

Машина ставится ПКМ предметом по блоку, капот смотрит туда же, куда игрок.

## Структура

```
avtovazmod/
├── build.gradle, gradle.properties, settings.gradle
├── tools/generate_assets.py          # текстуры, иконки, звук двигателя
└── src/main
    ├── java/ru/avtovazmod
    │   ├── AvtovazMod.java
    │   ├── registry/   ModEntities, ModItems, ModCreativeTabs, ModSounds
    │   ├── entity/     AbstractVazCarEntity, CarStats, Vaz2107Entity, VazNivaEntity
    │   ├── item/       AbstractCarSpawnItem, Vaz2107SpawnItem, VazNivaSpawnItem
    │   └── client/     ClientModEvents, CarClientHooks,
    │                   model/VazCarModel, renderer/*, sound/CarEngineSoundInstance
    └── resources
        ├── META-INF/   neoforge.mods.toml, accesstransformer.cfg
        ├── pack.mcmeta
        └── assets/avtovazmod/  lang, models/item, textures, sounds, sounds.json
```

## Как устроена физика
Подробности в javadoc `AbstractVazCarEntity`. Коротко: схема «как у лодки» — физику считает клиент водителя,
сервер принимает позицию, остальные интерполируют. Скорость раскладывается на продольную и боковую
составляющие; боковая гасится «сцеплением» — ручник и срыв задней оси его уменьшают, отсюда занос.
Характеристики моделей — в `CarStats` (`Vaz2107Entity.STATS`, `VazNivaEntity.STATS`).

## Известные упрощения
- Хитбокс — неповоротный квадрат 2×2 (ограничение AABB в Minecraft), кузов чуть короче по длине.
- Модель ванильная (`EntityModel`) с вращающимися колёсами и рулём. Для анимаций из Blockbench можно перейти на GeckoLib.
- Текстуры — сгенерированные заглушки.
