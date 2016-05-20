/*
 ** 2013 April 09
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema;

import java.io.File;

import info.ata4.minecraft.minema.client.cmd.CommandMinema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.modules.CaptureSession;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Main control class for Forge.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
@Mod(modid = Minema.ID, name = Minema.ID, version = Minema.VERSION, guiFactory = "info.ata4.minecraft.minema.client.config.MinemaConfigGuiFactory")
public final class Minema {

	public static final String ID = "Minema";
	public static final String VERSION = "1.9.4";

	@Instance(ID)
	public static Minema instance;

	private ModMetadata metadata;
	private MinemaConfig config;
	private CaptureSession session;

	@EventHandler
	public void onPreInit(final FMLPreInitializationEvent evt) {
		final File file = evt.getSuggestedConfigurationFile();
		this.config = new MinemaConfig(new Configuration(file));
		this.metadata = evt.getModMetadata();
	}

	@EventHandler
	public void onInit(final FMLInitializationEvent evt) {
		ClientCommandHandler.instance.registerCommand(new CommandMinema(this));
		MinecraftForge.EVENT_BUS.register(new KeyHandler(this));
		MinecraftForge.EVENT_BUS.register(this);
	}

	public MinemaConfig getConfig() {
		return this.config;
	}

	public ModMetadata getMetadata() {
		return this.metadata;
	}

	public void enable() {
		this.config.load();

		this.session = new CaptureSession(this.config);
		this.session.enable();
	}

	public void disable() {
		if (isEnabled()) {
			this.session.disable();
		}
		this.session = null;
	}

	public boolean isEnabled() {
		return this.session != null && this.session.isEnabled();
	}

	@SubscribeEvent
	public void onConfigChanged(final ConfigChangedEvent eventArgs) {
		if (ID.equals(eventArgs.getModID())) {
			this.config.update(false);
		}
	}

}
