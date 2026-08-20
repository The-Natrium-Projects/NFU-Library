### 0.x.33

NFU Registry refactor.

* Now entry values will get immutable after registry loading (previously called "generating").
* Register loading is mandatory now. There are three options of loading timing: on common setup, on side (client, server) setup, and on first access.
* Added a internal reverse map to improve the performance of getting key from value.

Removed a bunch of legacy classes and methods.

Completed NFU Entity Component System (NFU-ECS).

Refactored Mob Anger module to use NFU-ECS.

### 0.x.32

Added Entity Component system. Ported data, timer and anger handler to components.

Added Mixin Events: `EntityFinishConstructionEvent `, `EntityStuckInBlockEvent`.

### 0.x.31

Separated from NFF Services.

Added `CEntityDataCapability` transient parameter feature.

`MobApplicableItemTable` refactor. Now it's easier to be parsed externally.

Added feature to trade list: collection weight factor on registry entry. 

Trade entry data reading refactor.

Added `RegistrableFunction` and corresponding registry.

Added Mixin Events: `EffectCloudTakeEffectEvent`, `ThrownPotionAddEffectEvent`, `ThrownPotionEffectCloudEvent`

Added debug item: Tag Displayer.

Fixed `MobRespawnInfo` failing to save mob NBT when the mob is riding.

Added an option to fix vanilla flying mob speed issue (MC_172801) by mixin. Added related config to disable this fix.

Tweaked the trajectory of `NFUItemProjectileEntity` to compensate the gravity.

