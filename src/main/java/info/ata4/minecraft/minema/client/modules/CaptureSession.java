/*
 ** 2014 July 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.FrameEvent;
import info.ata4.minecraft.minema.client.event.FrameImportEvent;
import info.ata4.minecraft.minema.client.event.FrameInitEvent;
import info.ata4.minecraft.minema.client.modules.exporters.FrameExporter;
import info.ata4.minecraft.minema.client.modules.exporters.ImageFrameExporter;
import info.ata4.minecraft.minema.client.modules.exporters.PipeFrameExporter;
import info.ata4.minecraft.minema.client.modules.importers.FrameImporter;
import info.ata4.minecraft.minema.client.modules.modifiers.DisplaySizeModifier;
import info.ata4.minecraft.minema.client.modules.modifiers.GameSettingsModifier;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.client.util.CaptureFrame;
import info.ata4.minecraft.minema.client.util.CaptureTime;
import info.ata4.minecraft.minema.client.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class CaptureSession extends CaptureModule {

	private final ArrayList<CaptureModule> modules = new ArrayList<>();

	private Path captureDir;
	private String movieName;

	private CaptureTime time;
	private CaptureFrame frame;
	private int frameLimit;

	public CaptureSession(MinemaConfig cfg) {
		super(cfg);
	}

	@Override
	protected void doEnable() throws Exception {
		captureDir = Paths.get(cfg.capturePath.get());
		if (!Files.exists(captureDir)) {
			Files.createDirectories(captureDir);
		}
		movieName = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());

		if (!cfg.useVideoEncoder.get()) {
			Path movieDir = captureDir.resolve(movieName);
			if (!Files.exists(movieDir)) {
				Files.createDirectory(movieDir);
			}
		}

		frameLimit = cfg.frameLimit.get();

		// init modules
		modules.add(new GameSettingsModifier(cfg));

		if (cfg.syncEngine.get()) {
			if (Minecraft.getMinecraft().isSingleplayer()) {
				modules.add(new TimerModifier(cfg));
				modules.add(new TickSynchronizer(cfg));
			} else {
				ChatUtils.print("WARNING!!!", TextFormatting.RED);
				ChatUtils.print("Tick sync and shader sync is NOT going to work! Always record on a local world!",
						TextFormatting.RED);
			}
		}

		if (cfg.preloadChunks.get()) {
			if (!Minecraft.getMinecraft().isSingleplayer()) {
				ChatUtils.print("Warning!", TextFormatting.YELLOW);
				ChatUtils.print(
						"Instant chunk loading should be used on a local world! Only then it will be truly effective!",
						TextFormatting.YELLOW);
			}

			modules.add(new ChunkPreloader(cfg));
		}

		if (cfg.useFrameSize()) {
			modules.add(new DisplaySizeModifier(cfg));
		}

		modules.add(new FrameImporter(cfg));

		FrameExporter exporter;
		if (cfg.useVideoEncoder.get()) {
			exporter = new PipeFrameExporter(cfg);
		} else {
			exporter = new ImageFrameExporter(cfg);
		}
		modules.add(exporter);

		if (cfg.showOverlay.get()) {
			modules.add(new CaptureOverlay(cfg));
		}

		modules.add(new CaptureNotification(cfg));

		// enable and register modules
		modules.forEach(CaptureModule::enable);

		MinecraftForge.EVENT_BUS.register(this);

		// reset capturing state
		time = new CaptureTime(cfg.frameRate.get());
		frame = new CaptureFrame();

		postFrameEvent(new FrameInitEvent(frame, time, captureDir, movieName));
	}

	@Override
	protected void doDisable() {
		// disable and unregister modules
		modules.forEach(CaptureModule::disable);
		modules.clear();

		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@Override
	protected void handleError(Throwable throwable, String message, Object... args) {
		ChatUtils.print("minema.error.label", TextFormatting.RED);

		// get list of throwables and their causes
		List<Throwable> throwables = new ArrayList<>();
		do {
			throwables.add(throwable);
			throwable = throwable.getCause();
		} while (throwable != null);

		throwables.stream().filter(t -> {
			String msg = t.getMessage();

			// skip wrapped exceptions
			if (msg == null) {
				return false;
			}

			// skip wrapped exceptions with generated messages
			Throwable cause = t.getCause();
			return cause == null || !msg.equals(cause.toString());
		}).forEach(t -> ChatUtils.print(t.getMessage(), TextFormatting.RED));
	}

	@SubscribeEvent
	public void onRenderTick(RenderTickEvent e) {
		if (!isEnabled()) {
			return;
		}

		// only record at the end of the frame
		if (e.phase == Phase.START) {
			return;
		}

		// stop at frame limit
		if (frameLimit > 0 && time.getNumFrames() >= frameLimit) {
			disable();
		}

		// skip frames if the capturing framerate is not synchronized with the
		// rendering framerate
		if (!cfg.syncEngine.get() && !time.isNextFrame()) {
			// Game renders faster than necessary for recording?
			return;
		}

		postFrameEvent(new FrameImportEvent(frame, time, captureDir, movieName));
	}

	private void postFrameEvent(FrameEvent evt) {
		try {
			if (Minema.EVENT_BUS.post(evt)) {
				throw new RuntimeException("Frame capturing cancelled at frame " + time.getNumFrames());
			}
		} catch (Exception ex) {
			handleError(ex, "Frame capturing error");
			disable();
		}
	}
}
