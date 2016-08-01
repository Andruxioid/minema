/*
 ** 2014 July 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules.modifiers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.engine.FixedTimer;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class TimerModifier extends CaptureModule implements PrivateAccessor {

	private static final Logger L = LogManager.getLogger();
	private static final Minecraft MC = Minecraft.getMinecraft();

	private static FixedTimer timer = null;

	private float defaultTps;

	public TimerModifier(MinemaConfig cfg) {
		super(cfg);
	}

	@Override
	protected void doEnable() {
		Timer defaultTimer = minecraftGetTimer(MC);

		// check if it's modified already
		if (defaultTimer instanceof FixedTimer) {
			L.warn("Timer is already modified!");
			return;
		}

		// get default ticks per second if possible
		if (defaultTimer != null) {
			defaultTps = timerGetTicksPerSecond(defaultTimer);
		}

		float fps = cfg.frameRate.get().floatValue();
		float speed = cfg.engineSpeed.get().floatValue();

		// set fixed delay timer
		timer = new FixedTimer(defaultTps, fps, speed);
		minecraftSetTimer(MC, new FixedTimer(defaultTps, fps, speed));
	}

	@Override
	protected void doDisable() {
		// check if it's still modified
		if (!(minecraftGetTimer(MC) instanceof FixedTimer)) {
			L.warn("Timer is already restored!");
			return;
		}

		// restore default timer
		timer = null;
		minecraftSetTimer(MC, new Timer(defaultTps));
	}

	/**
	 * CALLED BY ASM INJECTED CODE! (COREMOD) DO NOT MODIFY METHOD SIGNATURE!
	 */
	public static void setFrameTimeCounter() {
		// This spot is right here because I can choose to only synchronize when
		// recording right here
		if (timer == null)
			return;
		timer.setFrameTimeCounter();
	}

}
