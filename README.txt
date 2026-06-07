================================================================================
 Aetas Ferrea Mod - Readme & Developer Information
================================================================================

Aetas Ferrea is a medieval survival mod built for Minecraft 1.20.1 and use 
of Aetas Ferrea modpack using the Minecraft Forge framework. 
It introduces realism-oriented mechanics to armor, combat physics, 
and equipment progression to enhance survival gameplay.

Features Overview
==============================

1. Golden Equipment Mechanics
   - Golden Dulling: Equipment attributes (attack damage, speed, mining speed) 
     degrade linearly as durability falls.
   - Innate Enchanting: Crafted/spawned gold gear automatically receives Looting II, 
     Fortune II, or Protection II to compensate for fragility.
   - Hero of the Village: Wearing a full set of gold armor grants the effect.

2. Armor Realism Matrix
   - Arrow Deflection: Projectiles have a scaling chance to bounce off heavier armor 
     (max 20% for full Iron, 60% for full Diamond).
   - Slashing Immunity: Full iron/diamond sets neutralize slashing weapon damage.
   - Blunt / High-Mass Bypass: Axes and blunt weapons deal extra armor-penetrating 
     damage to iron and chainmail wearers.
   - Durability Immunities: High-tier armors do not lose durability when struck by 
     lower-tier weapons.

3. Leather Armor Speed
   - Wearing leather pieces grants a speed boost (+1.25% per piece, capped at +5%).

4. Dynamic Config Menu
   - Full integration with Mod Menu, Configured, and YACL for configuration.

Setup & Compilation Process
==============================

To compile the mod, run the following command in the project root:

  Windows:  .\gradlew.bat build
  Bash:     ./gradlew build

This will compile the source code and resources into a production-ready mod JAR 
located in the `build/libs/` directory.

License & Credits
==============================
- License: MIT License
- Mod Logo: src/main/resources/aetasferrea_mod.png
