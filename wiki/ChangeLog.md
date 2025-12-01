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

