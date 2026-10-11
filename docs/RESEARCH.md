# RESEARCH — Этап A (завершён)

Все утверждения ниже подтверждены фактическим анализом байткода (`javap -p / -c`) классов,
распакованных из JAR в `.analysis/gv`, `.analysis/mca`, `.analysis/compat`, и метаданными
`META-INF/mods.toml`, извлечёнными из тех же JAR. Исходные Java-файлы недоступны — выводы по
логике методов сделаны из байткода (помечено «байткод»); сигнатуры классов/методов подтверждены
списками членов (`javap -p`).

## 0. Проверка входных файлов

Каталог `mods-for-analysis/` содержит ровно три JAR, версии совпадают с TASK.md:

| Файл | modId | Версия из mods.toml | Лицензия из mods.toml |
|---|---|---|---|
| guardvillagers-1.20.1-1.6.10.jar | `guardvillagers` | 1.6.10 | "MIT for the code, ARR for the assets" |
| minecraft-comes-alive-7.6.26-1.20.1-universal.jar | `mca` | 7.6.26+1.20.1 | GNU |
| mcamobcompat-forge-0.2.1+1.20.1.jar | `mcamobcompat` | 0.2.1 | GPL-3.0-only |

Важно: **ассеты Guard Villagers распространяются под ARR** (All Rights Reserved) — текстуры
стражников нельзя копировать в наш мод. Код GV — MIT. MCA — GNU (копируем код нельзя, только
используем API как зависимость). Mob Compat — GPL-3.0 (взаимодействуем только через события Forge,
не линкуемся).

## 1. Guard Villagers 1.6.10 (пакет `tallestegg.guardvillagers`)

### 1.1 Сущность Guard (подтверждено javap -p)
```
public class tallestegg.guardvillagers.entities.Guard extends PathfinderMob
  implements CrossbowAttackMob, RangedAttackMob, NeutralMob,
             ContainerListener, ReputationEventHandler
```
- **НЕ реализует** `VillagerDataHolder` и интерфейсы MCA (`VillagerLike`).
- Данные синхронизируются vanilla EntityDataAccessor'ами (например
  `OWNER_UNIQUE_ID: Optional<UUID>`).

### 1.2 Рендерер `client.renderer.GuardRenderer` (javap -p)
```
extends HumanoidMobRenderer<Guard, HumanoidModel<Guard>>
- private static final ResourceLocation guardTextures;   // статический блок: "guardvillagers:textures/entity/guard.png", суффикс "_steve" при конфиге guardSteve (байткод)
- public void render(Guard, ...)                          // вызывает setModelVisibilities + super.render
- private void setModelVisibilities(Guard)                // видимость quiver/наплечников
- private HumanoidModel.ArmPose getArmPose(Guard, ItemStack main, ItemStack off, InteractionHand)
    // ветки: ITEM / BLOCK(щит) / BOW_AND_ARROW / THROW_SPEAR / CROSSBOW_CHARGE / CROSSBOW_HOLD
    // + compat-ветка musketmod через ModCompat.reloadMusketAnim/holdMusketAnim (байткод)
- protected void scale(Guard, PoseStack, float)           // ldc 0.9375f x3 → poseStack.scale(0.9375,0.9375,0.9375) (байткод)
- public ResourceLocation getTextureLocation(Guard)       // guardTextures (+ вариант через GuardVariantLayer)
- Внутренний класс GuardRenderer$GuardVariantLayer extends RenderLayer — рисует слой варианта
  стража (getGuardVariant()) поверх основной текстуры. Это НЕ ArmorLayer.
```

### 1.3 Модели (javap -p)
- `client.models.GuardModel extends HumanoidModel<Guard>`:
  публичные поля `Nose` (!), `quiver`, `ArmLShoulderPad`, `ArmRShoulderPad`;
  метод `createBodyLayer()` (LayerDefinition), `setupAnim`, `eatingAnimation*`, `holdWeaponHigh`.
- `client.models.GuardSteveModel extends PlayerModel<Guard>` — «стивов»-вариант без носа
  (выбор через конфиг `guardSteve`, байткод конструктора рендерера).
- `client.models.GuardArmorModel extends HumanoidModel<Guard>` — броня outer/inner.

### 1.4 Регистрация (ключевой факт)
`tallestegg.guardvillagers.client.GuardClientEvents`:
```
public static ModelLayerLocation GUARD, GUARD_STEVE, GUARD_ARMOR_OUTER, GUARD_ARMOR_INNER;
public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions);
public static void entityRenderers(EntityRenderersEvent.RegisterRenderers);
```
Зарегистрирован через `@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD)` (байткод
аннотаций класса). Т.е. GV использует **штатное событие Forge** — значит и мы можем зарегистрировать
свой рендерер для того же EntityType во втором обработчике того же события:
`EntityRenderers.put` вызывается в момент dispatch, последний победитель определяет рендерер
(порядок обработчиков = порядок загрузки модов; гарантируется зависимостью `after` в нашем mods.toml).

## 2. MCA Reborn 7.6.26 (пакет `forge.net.mca`)

### 2.1 Обнаруженные классы (javap -p)
- `forge.net.mca.client.render.VillagerLikeEntityMCARenderer<T extends Mob & VillagerLike<T>>
   extends HumanoidMobRenderer<T, VillagerEntityModelMCA<T>>`
  + `VillagerEntityMCARenderer`, `ZombieVillagerEntityMCARenderer`, слои `SkinLayer`, `HairLayer`,
  `FaceLayer`, `ClothingLayer`, `VillagerLayer`.
- `forge.net.mca.client.model.VillagerEntityModelMCA<T extends LivingEntity & VillagerLike<T>>
   extends VillagerEntityBaseModelMCA<T>` (поля breastsWear, left/right Armwear, Legwear…).
- Интерфейс `forge.net.mca.entity.VillagerLike<E extends Entity & VillagerLike<E>> extends
  CTrackedEntity<E>, VillagerDataHolder, Infectable, Messenger` + методы brain/genetics/traits/clothes/hair.
  Текстура/внешность берутся из CDP-параметров `CUSTOM_SKIN`, `CLOTHES`, `HAIR` (статические поля интерфейса).

### 2.2 Главный вывод
Модель и рендерер MCA **жёстко привязаны дженериками к `VillagerLike<T>`**. Класс `Guard` этот
интерфейс не реализует и не может быть им дополнен без патча чужого JAR (запрещено TASK).
⇒ **Прямое переиспользование `VillagerEntityModelMCA` / `VillagerLikeEntityMCARenderer` для Guard
технически невозможно.** Единственный легальный путь — собственный клиентский адаптер (см. §4).

### 2.3 Регистрация MCA (важно для порядка)
`forge.net.mca.forge.MCAForgeClient.setup(FMLClientSetupEvent)` вызывает напрямую
`EntityRenderers.m_174036_(type, provider)` (обфусцированное `register`) для своих типов — то есть
MCA регистрирует рендереры **вне** `RegisterRenderers`-события. На `guardvillagers:guard` это не
влияет; конфликт возможен только если другой мод перерегистрирует тот же тип позже нас.

## 3. MCA: Mob Compat 0.2.1 (пакет `dev.otectus.mcamobcompat`)

- Архитектура «прокси»: `bridge.GuardProxy` создаёт обычного ванильного `Villager` с профессией
  `GUARD_PROFESSION` (RL), связывая его с Guard по UUID (`KEY_PROXY`/`KEY_GUARD`, методы
  `isProxy/nearbyGuardOf/proxyOf/guardOf/create`, радиус `ADOPT_RANGE`, `CREATE_GRACE_TICKS`).
- Клиент: `client.ProxyRenderHandler` — слушатель `RenderLivingEvent.Pre` (FORGE-bus, Dist.CLIENT):
  если рисуется прокси — отменяет рендер и вызывает `drawInsteadOf(...)`: временно копирует
  позицию/углы Guard'а на прокси и рендерит **proxied Villager штатным MCA-рендерером**
  (`EntityRenderDispatcher.getRenderer(...).render(...)`), с защитой от рекурсии (флаг
  `substituting` + StackWalker по кадру экрана).
- `api.*` (GuardIdentity, MobCompatibilityAPI…), `classification.*`, `mca.McaBinding` — серверная
  идентичность/соцданные; к нам претензий не имеют.
- Зависимостей от классов GV в compile-time нет (обращение через reflection/registry ids).

### Вывод по совместимости
Наш мод меняет **только рендерер сущности `guardvillagers:guard`**. Proxy-Villager продолжит
рендериться своим обычным способом (невидимым/подменённым) — двойных тел не появится, но это
требует игровой проверки (см. риски). Систему Mob Compat мы не трогаем вообще.

## 4. Выбранная архитектура нового мода (`mcaguardappearance`)

Клиентский Forge-мод 1.20.1 (Java 17, ForgeGradle/MDK 47.x):

1. `GuardAppearanceMod` — @Mod, регистрация событий на MOD-bus.
2. `HumanGuardModel extends HumanoidModel<Guard>` — человеческая геометрия **без носа**,
   построенная стандартными билдерами Minecraft (`ModelPart`/`HeadMesh` и т.д.), пропорции
   близки к player-модели (совместимые с MCA-стилем); опционально quiver-part для parity.
   Собственная текстура-скин (ARR-совместимо: рисуем сами, 64x64 player-style UV).
3. `HumanGuardRenderer extends HumanoidMobRenderer<Guard, HumanGuardModel>`:
   - копия логики `scale(0.9375)` (число — факт байткода, не ассет);
   - собственная `getArmPose`-логика (ITEM/BLOCK/BOW/CROSSBOW/SPEAR) — поведение поз,
     реализованное независимо (MIT-логика GV не копируется дословно, API ArmPose — vanilla);
   - `addLayer HumanoidArmorLayer` для брони (стандартный vanilla-механизм);
   - `getTextureLocation` — наша текстура.
4. Регистрация: обработчик `EntityRenderersEvent.RegisterLayerDefinitions` +
   `EntityRenderersEvent.RegisterRenderers`, где `event.registerEntityRenderer(GuardEntityType, ...)`
   выполняется **после** GV ⇒ dependency `[[dependencies.mcaguardappearance]] modId="guardvillagers"
   ordering="AFTER"` в mods.toml. Никакого reflection/mixin.
5. Graceful degradation: если GV не установлен — ни один класс не грузится (все ссылки на Guard
   только внутри renderer/model, они не трогаются без регистрации типа).

### Список файлов этапа B+
build.gradle, settings.gradle, gradle.properties, gradle wrapper, src/main/resources/META-INF/mods.toml,
pack.mcmeta, пакет `com.github.bedwulf.mcaguardappearance` (мод-класс, модель, рендерер, клиент-события),
текстура, README, docs/TESTING.md.

## 5. Риски (не разрешены полностью)

1. **Порядок двух подписчиков RegisterRenderers** — по модели Forge последний выигрывает, но
   точный порядок при `ordering="AFTER"` требует подтверждения в реальной игре (Minecraft здесь
   запустить нельзя).
2. **Совмещение с proxy-подменой Mob Compat** — нужен игровой тест отсутствия двойных тел/артефактов.
3. **Точность масштаба и позиций рук с оружием** — визуальная настройка в игре.
4. **Лицензии**: ассеты GV (ARR) не используются; код MCA/GV не копируется — только независимая
   реализация на vanilla-API.
5. Игровая среда недоступна в этом окружении → пункты 3–13 TESTING будут NOT TESTED/BLOCKED честно.
