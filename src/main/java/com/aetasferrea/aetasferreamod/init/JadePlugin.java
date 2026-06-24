/**
 * @file JadePlugin.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Jade (Waila) plugin implementation to display custom HUD tooltips for medieval horses.
 *
 * @description
 * Implements the Jade plugin to register client and server side tooltip integration.
 * Displays details such as horse class name, breaking progress, daily food limit, and specialization training progress.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.init;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import snownee.jade.api.IWailaClientRegistration;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

// ---------- CLASS
@WailaPlugin(AetasFerreaMod.MODID)
public class JadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(EquineComponentProvider.INSTANCE, AbstractHorse.class);
    }
}
