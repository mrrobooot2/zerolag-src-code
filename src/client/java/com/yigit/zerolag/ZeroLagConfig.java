package com.yigit.zerolag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public final class ZeroLagConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public boolean enabled = true;
	public FramePacing framePacing = FramePacing.SMART;
	public VsyncMode vsyncMode = VsyncMode.LOW_LATENCY;
	public boolean lateInput = true;
	public boolean tapBuffer = true;
	public InstantMode instantActions = InstantMode.FULL;
	public boolean instantCombatOnServers = false;

	public FramePacing effectiveFramePacing() {
		return this.enabled ? this.framePacing : FramePacing.OFF;
	}

	public VsyncMode effectiveVsyncMode() {
		return this.enabled ? this.vsyncMode : VsyncMode.VANILLA;
	}

	public boolean lateInputActive() {
		return this.enabled && this.lateInput;
	}

	public boolean tapBufferActive() {
		return this.enabled && this.tapBuffer;
	}

	public InstantMode effectiveInstantActions(final boolean remoteServer) {
		if (!this.enabled) {
			return InstantMode.OFF;
		}

		if (this.instantActions == InstantMode.FULL && remoteServer && !this.instantCombatOnServers) {
			return InstantMode.SAFE;
		}

		return this.instantActions;
	}

	public static ZeroLagConfig load(final Path path) throws IOException {
		if (Files.notExists(path)) {
			ZeroLagConfig created = new ZeroLagConfig();
			created.save(path);
			return created;
		}

		ZeroLagConfig loaded;
		try (Reader reader = Files.newBufferedReader(path)) {
			loaded = GSON.fromJson(reader, ZeroLagConfig.class);
		}

		if (loaded == null) {
			return new ZeroLagConfig();
		}

		// Gson leaves unknown enum names as null.
		ZeroLagConfig defaults = new ZeroLagConfig();
		if (loaded.framePacing == null) {
			loaded.framePacing = defaults.framePacing;
		}

		if (loaded.vsyncMode == null) {
			loaded.vsyncMode = defaults.vsyncMode;
		}

		if (loaded.instantActions == null) {
			loaded.instantActions = defaults.instantActions;
		}

		return loaded;
	}

	public void save(final Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temporary)) {
			GSON.toJson(this, writer);
		}

		try {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public enum FramePacing implements StringRepresentable {
		@SerializedName("off")
		OFF,
		@SerializedName("smart")
		SMART,
		@SerializedName("strict")
		STRICT;

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public enum VsyncMode implements StringRepresentable {
		@SerializedName("vanilla")
		VANILLA,
		@SerializedName("low_latency")
		LOW_LATENCY,
		@SerializedName("mailbox")
		MAILBOX;

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public enum InstantMode implements StringRepresentable {
		@SerializedName("off")
		OFF,
		@SerializedName("safe")
		SAFE,
		@SerializedName("full")
		FULL;

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
