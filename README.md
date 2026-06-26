# Aetas Ferrea Mod

Aetas Ferrea is a medieval survival mod built for Minecraft 1.20.1 using the Minecraft Forge framework. It introduces realism-oriented mechanics to armor, combat physics, and equipment progression to enhance survival gameplay.

## Features Overview

### 1. Golden Equipment Mechanics

- Golden Dulling: Equipment attributes such as attack damage, speed, and mining speed degrade linearly as durability falls.
- Innate Enchanting: Crafted and spawned gold gear automatically receives Looting II, Fortune II, or Protection II to compensate for fragility.
- Hero of the Village: Wearing a full set of gold armor grants the effect.
- Grindstone Disenchanting Lock: These innate golden enchantments and custom fantasy armor sets cannot be disenchanted or processed with a grindstone.

### 2. Armor Realism Matrix

- Arrow Deflection: Projectiles have a scaling chance to bounce off heavier armor, with a maximum of 20% for a full iron set and 60% for a full diamond set.
- Slashing Immunity: Full iron and diamond sets neutralize slashing weapon damage.
- Blunt / High-Mass Bypass: Axes and blunt weapons deal extra armor-penetrating damage to iron and chainmail wearers.
- Durability Immunities: High-tier armor does not lose durability when struck by lower-tier weapons.
- Combat Roll Weight: Integrates with the Combat Roll mod; heavy armor penalizes roll count and recharge speed, while leather and light armor grant bonuses.
- Primitive Durability Penalties: Striking heavy armor with wooden or stone weapons inflicts high durability damage on the attacking primitive weapon.
- Fist Immunity: Punching certain heavy armor bare-handed, including against mobs, deals zero damage to the defender.

### 3. Leather Armor Speed

- Wearing leather pieces grants a speed boost of +1.25% per piece, capped at +5%.

### 4. Dynamic Config Menu

- Full integration with Mod Menu, Configured, and YACL for configuration.

### 5. Equine / Mounted Combat Overhaul

- Class-Based Horses: Modifies horses into five distinct classes — Wild, Rouncey, Destrier, Courser, and Palfrey — each with unique attribute caps and stat scaling.
- Realistic Riding Controls: Features sigmoid throttle-based acceleration, walk-mode speed caps, reversing, and fall damage when jumping off at high speeds.
- Hand-Feeding Taming: Wild horses and donkeys must be leashed and fed daily up to four times using wheat, sugar, hay, apples, or golden carrots until broken.
- Class Specializations & Upgrades: Tamed Rounceys accumulate combat and agility XP to promote into specialized war Destriers, swift Coursers, or docile Palfreys.
- Custom Saddlebags: Palfreys, Rounceys, donkeys, and mules can carry chests for custom inventory storage, supporting up to 9, 27, and 54 slots respectively.
- Aquatic Behavior & Buoyancy: Mounts sink when loaded with heavy armor or chests. Donkeys panic and throw riders in deep water, while mules drown but stay loyal.

### 6. Wolf AI & Pack Defense Mechanics

- Strict Follow Goal: Replaces vanilla AI with quiet sneaking or stealth behaviors, dynamic target clearing, and anti-stuck teleportation.
- Aggressive Predators: Wild wolves have a 25% chance to spawn as player-hostile predators that spot and hunt players within range.
- Tactical Retreat & Cooldowns: Wolves retreat and gain Speed II when health falls below 30%. Combat attacks apply stuns on alternate strikes with brief post-attack slows.
- Friendly Fire & Prey Drive: Prevents owner or pack friendly fire unless crouching, and restricts tamed wolves from attacking passive prey unless ordered.
- Pack Limit: Taming is capped at two active wolves. Excess tamed wolves reject the pack, become wild predators, and target the owner.
- Hound Threat Auto-Targeting: Standing tamed hounds auto-target nearby hostile threats, excluding Creepers, within 10 blocks of the owner.

### 7. Regional & Progression Difficulty Engine

- World Age Mob Caps: Throttles surface hostile spawns based on days, with a cap of 12 early on scaling to 60 after Day 31.
- Equipment Progression: Hostiles spawn gearless early on Days 0–3, with shields or stone tools later on Days 6–7, and leather or chainmail scout variants from Day 11 onward.
- Banishment & Conversion Limits: Permanently bans Creepers and Witches. Day 0 bans Skeletons, Vindicators, and Drowned, and prevents zombie conversion before Day 5.
- No Baby Zombie: Automatically ages baby zombies to adults upon spawning.
- Dragon Progression Smithing Lock: Prevents smithing legendary endgame gear such as Ornstein and Dragonslayer until the Ender Dragon advancement is unlocked.

### 8. Progressive Night Mini-Bosses

- Night Spawners: Skeletons and husks have a chance to spawn as progressive bosses at night.
- Catena-Mail Vigil: Skeleton archer boss with 60 HP, glowing status, stationary firing, and periodic guard summoning.
- Defiled Castellan: Husk boss with 80 HP that inflicts Mining Fatigue, Weakness, massive knockback, and shield-bypassing attacks.
- Dead Iron Knight: Husk boss with 100 HP, four sapper squad guards, and the ability to break blocks in its path.
- Diamond Knight: Rare husk boss with 150 HP that sequentially sheds its armor pieces as health decreases, gaining permanent movement speed boosts.

### 9. Ecological & Environmental Balance

- Biome-Specific Fishing: Restricts caught fish drops to native biomes, such as Cod or Salmon in oceans and rivers and Pufferfish or Tropical Fish in jungles or warm oceans, or yields junk otherwise.
- Squid Beaching & Puddle Protection: Prevents squids from spawning in shallow puddles and pushes beaching squids back into water blocks.
- Spider Spawning Limits: Spiders can only spawn on the surface in forest and jungle biomes.
- Pufferfish Inflation Exhaustion: Pufferfish suffer from stamina limits, forcing deflation and cooling down after long inflations.
- Chicken Fatigue & Fall Damage: Chickens suffer wing fatigue, lose fall drag, and take fall damage when falling more than 8 blocks.
- Fox Holding Restrictions: Wild and tamed foxes can only hold items specified in the custom `aetasferreamod:fox_holdable` item tag in their mouths.

### 10. Medieval Barter & Progression Economy

- Currency Reforms: Emeralds are replaced by copper for peasants, raw iron for merchants, and raw gold for mystics or knights based on villager profession and level.
- Material Tier Upgrades: Villagers require the previous material tier of armor or tools, such as chainmail for iron or diamond, to trade for higher-tier equipment.
- Wandering Trader Overhaul: Offers exotic and rare items, such as saddles, blaze rods, and totems of undying, in exchange for emeralds.

### 11. Bare-Handed Harvest Restrictions

- Trauma & Fatigue Penalties: Punching or mining blocks that require tools bare-handed inflicts Mining Fatigue III, direct health or limb damage, and breaks incorrect tools.
- Knife Wood-Chopping: Knives can be used as a slow alternative to axes to chop logs, consuming high durability and granting brief Haste.

### 12. Client-Side HUD & Tooltip Enhancements

- Jade (Waila) Integration: Displays detailed custom HUD tooltips for horses, including class name, temper or breaking progress, daily feed count, and specialization training progress.
- Re-formatted Attribute Tooltips: Client-side reformatting of item attribute tooltips aligns with vanilla standards, sorting by slot and using correct absolute or delta values while formatting minor values cleanly.

### 13. Thermodynamic & Burning Combat Mechanics

- Lava Quenching: Going underwater while holding a lava bucket quenches it into an empty bucket and spawns obsidian at the player's position.
- Panic Speed: Being on fire triggers a flight or panic response, granting a temporary movement speed boost of Speed I.
- Burning Weapon Attacks: Designated items in the `aetasferreamod:burning_items` tag inflict burning fire ticks for five seconds when attacking targets.

## Setup & Compilation Process

To compile the mod, run the following command in the project root:

### Windows

```powershell
.\gradlew.bat build
```

### Bash

```bash
./gradlew build
```

This will compile the source code and resources into a production-ready mod JAR located in `build/libs/`.

## License & Credits

- License: MIT License
- Mod Logo: `src/main/resources/aetasferrea_mod.webp`
