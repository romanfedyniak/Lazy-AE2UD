/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Everything a pack can change, in {@code config/lazy_ae2.cfg}. Each machine has a section of its own, and
 * switching one off takes its blocks and their recipes with it.
 * <p>
 * The file keeps the name the old mod used, so a server that had tuned its numbers is read once and carried
 * over into the new sections; see {@link #legacy}.
 */
public final class LazyAE2Config extends Configuration {

    public static final String MATERIALS = "materials";
    public static final String AGGREGATOR = "aggregator";
    public static final String CENTRIFUGE = "centrifuge";
    public static final String ETCHER = "etcher";
    public static final String ENERGIZER = "energizer";
    public static final String PAU = "pau";
    public static final String LEVEL_MAINTAINER = "levelMaintainer";
    public static final String MASS_ASSEMBLER = "massAssembler";

    private static final String LEGACY_ROOT = "general";
    private static final String LEGACY_PROCESSING = "general.processing";
    private static final String LEGACY_DEVICES = "general.networkdevices";
    private static final String LEGACY_ASSEMBLER = "general.massassembler";

    private static LazyAE2Config instance;

    private final boolean coalDust;

    /** How many Acceleration Cards each machine takes, and the most upgrade points it counts. */
    private final Map<String, Integer> speedCards = new HashMap<>();
    private final Map<String, Integer> speedPoints = new HashMap<>();
    /** The same for the Pattern Expansion Cards, which the machines holding patterns take instead. */
    private final Map<String, Integer> patternCards = new HashMap<>();
    private final Map<String, Integer> patternPoints = new HashMap<>();

    private final Processor aggregator;
    private final Processor centrifuge;
    private final Processor etcher;
    private final Processor energizer;

    private final boolean pauEnabled;
    private final double pauIdlePower;

    private final boolean levelMaintainerEnabled;
    private final boolean levelMaintainerTerminalEnabled;
    private final double levelMaintainerIdlePower;
    private final int levelMaintainerSleepMin;
    private final int levelMaintainerSleepMax;
    private final int levelMaintainerRetryTicks;

    private final boolean massAssemblerEnabled;
    private final double massAssemblerIdlePower;
    private final int massAssemblerJobQueueSize;
    private final int massAssemblerWorkPerJob;
    private final double massAssemblerEnergyPerWorkBase;
    private final double massAssemblerEnergyPerWorkUpgrade;
    private final int massAssemblerWorkPerTickBase;
    private final int massAssemblerWorkPerTickUpgrade;

    private LazyAE2Config(final File file) {
        super(file);

        this.coalDust = this.get(MATERIALS, "coalDust", true,
                "Whether this mod's Coal Dust is offered. Switched off, it is hidden and nothing of ours is "
                        + "registered as dustCoal, so coal dust comes from another mod instead - and with no such "
                        + "mod installed there is no way to make fluix steel.").getBoolean();

        this.aggregator = this.readProcessor(AGGREGATOR, "aggregator", "Fluix Aggregator", true);
        this.centrifuge = this.readProcessor(CENTRIFUGE, "centrifuge", "Pulse Centrifuge", true);
        this.etcher = this.readProcessor(ETCHER, "etcher", "ME Circuit Etcher", true);
        this.energizer = this.readProcessor(ENERGIZER, "energizer", "Crystal Energizer", false);

        this.pauEnabled = this.get(PAU, "enabled", true,
                "Whether the Preemptive Assembly Unit and its recipe exist at all.").getBoolean();
        this.pauIdlePower = Math.max(0, this.get(PAU, "idlePower",
                this.legacyDouble(LEGACY_DEVICES, "fastCrafterIdlePower", 6D),
                "Power the unit draws while doing nothing (AE/t).").getDouble());

        this.levelMaintainerEnabled = this.get(LEVEL_MAINTAINER, "enabled", true,
                "Whether the ME Level Maintainer and its recipe exist at all.").getBoolean();
        this.levelMaintainerTerminalEnabled = this.get(LEVEL_MAINTAINER, "terminal", true,
                "Whether the ME Level Maintainer Terminal, its recipe and the wireless mode it unlocks exist "
                        + "at all. A terminal is no use without maintainers, so switching the machine off "
                        + "takes the terminal with it either way.").getBoolean();
        this.levelMaintainerIdlePower = Math.max(0, this.get(LEVEL_MAINTAINER, "idlePower",
                this.legacyDouble(LEGACY_DEVICES, "levelMaintainerIdlePower", 3D),
                "Power the maintainer draws while doing nothing (AE/t).").getDouble());
        this.levelMaintainerSleepMin = Math.max(0, this.get(LEVEL_MAINTAINER, "sleepMin",
                this.legacyInt(LEGACY_DEVICES, "levelMaintainerSleepMin", 12),
                "The shortest interval between work ticks, in ticks. The maintainer speeds up while nothing "
                        + "gets in its way. Too low costs server time.").getInt());
        this.levelMaintainerSleepMax = Math.max(this.levelMaintainerSleepMin, this.get(LEVEL_MAINTAINER, "sleepMax",
                this.legacyInt(LEGACY_DEVICES, "levelMaintainerSleepMax", 200),
                "The longest interval between work ticks, in ticks. The maintainer slows down while something "
                        + "stops it making progress.").getInt());
        this.levelMaintainerRetryTicks = Math.max(0, this.get(LEVEL_MAINTAINER, "retryTicks", 200,
                "How long a row waits before planning again after the network turned it down, in ticks. "
                        + "Without it a row asking for something nothing can supply plans a job every work "
                        + "tick for as long as the machine stands. Editing the row, a change in what it "
                        + "watches, or opening a window that shows it ends the wait at once.").getInt());

        this.massAssemblerEnabled = this.get(MASS_ASSEMBLER, "enabled", true,
                "Whether the Mass Assembly Chamber's blocks and their recipes exist at all.").getBoolean();
        this.massAssemblerIdlePower = Math.max(0, this.get(MASS_ASSEMBLER, "idlePower",
                this.legacyDouble(LEGACY_ASSEMBLER, "idlePower", 3D),
                "Power the chamber draws while doing nothing (AE/t).").getDouble());
        this.massAssemblerJobQueueSize = Math.max(1, this.get(MASS_ASSEMBLER, "jobQueueSize",
                this.legacyInt(LEGACY_ASSEMBLER, "jobQueueSize", 64),
                "How many crafting jobs the chamber queues. Lowering this can lose queued jobs.").getInt());
        this.massAssemblerWorkPerJob = Math.max(1, this.get(MASS_ASSEMBLER, "workPerJob",
                this.legacyInt(LEGACY_ASSEMBLER, "workPerJob", 16),
                "How much work one crafting job takes.").getInt());
        this.massAssemblerEnergyPerWorkBase = Math.max(0, this.get(MASS_ASSEMBLER, "energyPerWorkBase",
                this.legacyDouble(LEGACY_ASSEMBLER, "energyPerWorkBase", 16D),
                "Power one unit of work costs (AE).").getDouble());
        this.massAssemblerEnergyPerWorkUpgrade = Math.max(0, this.get(MASS_ASSEMBLER, "energyPerWorkUpgrade",
                this.legacyDouble(LEGACY_ASSEMBLER, "energyPerWorkUpgrade", 1D),
                "Power each co-processor adds to one unit of work (AE).").getDouble());
        this.massAssemblerWorkPerTickBase = Math.max(0, this.get(MASS_ASSEMBLER, "workPerTickBase",
                this.legacyInt(LEGACY_ASSEMBLER, "workPerTickBase", 1),
                "How much work the chamber does each tick with no co-processor installed. Zero makes a chamber "
                        + "without one do nothing at all.").getInt());
        this.massAssemblerWorkPerTickUpgrade = Math.max(1, this.get(MASS_ASSEMBLER, "workPerTickUpgrade",
                this.legacyInt(LEGACY_ASSEMBLER, "workPerTickUpgrade", 3),
                "How much work each co-processor adds to a tick.").getInt());

        this.setCategoryComment("upgrades.cards", "How many cards of a kind fit in each machine. Zero refuses "
                + "the card there outright.");
        this.setCategoryComment("upgrades.points", "The most points of an upgrade a machine counts, however many "
                + "its cards carry. Zero lets it take them all.");
        for (final String machine : new String[] { AGGREGATOR, CENTRIFUGE, ETCHER, ENERGIZER }) {
            this.speedCards.put(machine, Math.max(0, this.get("upgrades.cards", "speed." + machine, 8).getInt()));
            this.speedPoints.put(machine, Math.max(0, this.get("upgrades.points", "speed." + machine, 0).getInt()));
        }
        this.patternCards.put(PAU, Math.max(0, this.get("upgrades.cards", "patterns." + PAU, 3).getInt()));
        this.patternPoints.put(PAU, Math.max(0, this.get("upgrades.points", "patterns." + PAU, 0).getInt()));

        this.dropLegacy();
    }

    private Processor readProcessor(final String section, final String legacyPrefix, final String machine,
            final boolean withCostBase) {
        final boolean enabled = this.get(section, "enabled", true,
                "Whether the " + machine + " and its recipe exist at all.").getBoolean();
        final int buffer = Math.max(1, this.get(section, "energyBuffer",
                this.legacyInt(LEGACY_PROCESSING, legacyPrefix + "EnergyBuffer", 100000),
                "How much energy the " + machine + " stores (FE).").getInt());
        final int costBase = withCostBase
                ? Math.max(0, this.get(section, "energyCostBase",
                        this.legacyInt(LEGACY_PROCESSING, legacyPrefix + "EnergyCostBase", 8100),
                        "Energy one operation costs (FE).").getInt())
                : 0;
        final int costUpgrade = Math.max(0, this.get(section, "energyCostUpgrade",
                this.legacyInt(LEGACY_PROCESSING, legacyPrefix + "EnergyCostUpgrade", withCostBase ? 863 : 1625),
                "Energy each Acceleration Card adds to one operation (FE).").getInt());
        final int ticksBase = Math.max(1, this.get(section, "workTicksBase",
                this.legacyInt(LEGACY_PROCESSING, legacyPrefix + "WorkTicksBase", 150),
                "Ticks one operation takes with no Acceleration Card.").getInt());
        final int ticksUpgrade = Math.max(0, this.get(section, "workTicksUpgrade",
                this.legacyInt(LEGACY_PROCESSING, legacyPrefix + "WorkTicksUpgrade", 18),
                "Ticks each Acceleration Card takes off one operation.").getInt());
        return new Processor(enabled, buffer, costBase, costUpgrade, ticksBase, ticksUpgrade);
    }

    /**
     * A value of the mod this one replaces, or {@code null} once its categories are gone.
     */
    @Nullable
    private Property legacy(final String category, final String key) {
        if (!this.hasCategory(category)) {
            return null;
        }
        final ConfigCategory old = this.getCategory(category);
        return old.containsKey(key) ? old.get(key) : null;
    }

    private int legacyInt(final String category, final String key, final int fallback) {
        final Property property = this.legacy(category, key);
        return property == null ? fallback : property.getInt(fallback);
    }

    private double legacyDouble(final String category, final String key, final double fallback) {
        final Property property = this.legacy(category, key);
        return property == null ? fallback : property.getDouble(fallback);
    }

    /**
     * Everything the old sections had has been read into the new ones by now, so they go.
     */
    private void dropLegacy() {
        for (final String category : new String[] { LEGACY_PROCESSING, LEGACY_DEVICES, LEGACY_ASSEMBLER }) {
            if (this.hasCategory(category)) {
                this.removeCategory(this.getCategory(category));
            }
        }
        if (this.hasCategory(LEGACY_ROOT)) {
            final ConfigCategory root = this.getCategory(LEGACY_ROOT);
            if (root.isEmpty() && root.getChildren().isEmpty()) {
                this.removeCategory(root);
            }
        }
    }

    public static void init(final File configDirectory) {
        instance = new LazyAE2Config(new File(configDirectory, "lazy_ae2.cfg"));
        if (instance.hasChanged()) {
            instance.save();
        }
    }

    public static LazyAE2Config instance() {
        return instance;
    }

    /**
     * For recipe conditions, which name a machine by its config section.
     */
    public boolean isEnabled(final String machine) {
        switch (machine) {
            case AGGREGATOR:
                return this.aggregator.enabled;
            case CENTRIFUGE:
                return this.centrifuge.enabled;
            case ETCHER:
                return this.etcher.enabled;
            case ENERGIZER:
                return this.energizer.enabled;
            case PAU:
                return this.pauEnabled;
            case LEVEL_MAINTAINER:
                return this.levelMaintainerEnabled;
            case MASS_ASSEMBLER:
                return this.massAssemblerEnabled;
            default:
                return false;
        }
    }

    public int getSpeedCards(final String machine) {
        return this.speedCards.getOrDefault(machine, 0);
    }

    public int getSpeedPoints(final String machine) {
        return this.speedPoints.getOrDefault(machine, 0);
    }

    public int getPatternCards(final String machine) {
        return this.patternCards.getOrDefault(machine, 0);
    }

    public int getPatternPoints(final String machine) {
        return this.patternPoints.getOrDefault(machine, 0);
    }

    public boolean isCoalDustEnabled() {
        return this.coalDust;
    }

    public Processor getAggregator() {
        return this.aggregator;
    }

    public Processor getCentrifuge() {
        return this.centrifuge;
    }

    public Processor getEtcher() {
        return this.etcher;
    }

    public Processor getEnergizer() {
        return this.energizer;
    }

    public boolean isPauEnabled() {
        return this.pauEnabled;
    }

    public double getPauIdlePower() {
        return this.pauIdlePower;
    }

    public boolean isLevelMaintainerEnabled() {
        return this.levelMaintainerEnabled;
    }

    public boolean isLevelMaintainerTerminalEnabled() {
        return this.levelMaintainerEnabled && this.levelMaintainerTerminalEnabled;
    }

    public double getLevelMaintainerIdlePower() {
        return this.levelMaintainerIdlePower;
    }

    public int getLevelMaintainerSleepMin() {
        return this.levelMaintainerSleepMin;
    }

    public int getLevelMaintainerSleepMax() {
        return this.levelMaintainerSleepMax;
    }

    public int getLevelMaintainerRetryTicks() {
        return this.levelMaintainerRetryTicks;
    }

    public boolean isMassAssemblerEnabled() {
        return this.massAssemblerEnabled;
    }

    public double getMassAssemblerIdlePower() {
        return this.massAssemblerIdlePower;
    }

    public int getMassAssemblerJobQueueSize() {
        return this.massAssemblerJobQueueSize;
    }

    public int getMassAssemblerWorkPerJob() {
        return this.massAssemblerWorkPerJob;
    }

    public double getMassAssemblerEnergyPerWorkBase() {
        return this.massAssemblerEnergyPerWorkBase;
    }

    public double getMassAssemblerEnergyPerWorkUpgrade() {
        return this.massAssemblerEnergyPerWorkUpgrade;
    }

    public int getMassAssemblerWorkPerTickBase() {
        return this.massAssemblerWorkPerTickBase;
    }

    public int getMassAssemblerWorkPerTickUpgrade() {
        return this.massAssemblerWorkPerTickUpgrade;
    }

    /**
     * One of the four machines that turn a recipe into an item on their own power.
     */
    public static final class Processor {

        private final boolean enabled;
        private final int energyBuffer;
        private final int energyCostBase;
        private final int energyCostUpgrade;
        private final int workTicksBase;
        private final int workTicksUpgrade;

        private Processor(final boolean enabled, final int energyBuffer, final int energyCostBase,
                final int energyCostUpgrade, final int workTicksBase, final int workTicksUpgrade) {
            this.enabled = enabled;
            this.energyBuffer = energyBuffer;
            this.energyCostBase = energyCostBase;
            this.energyCostUpgrade = energyCostUpgrade;
            this.workTicksBase = workTicksBase;
            this.workTicksUpgrade = workTicksUpgrade;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public int getEnergyBuffer() {
            return this.energyBuffer;
        }

        /** Zero for the Crystal Energizer, whose recipes carry a cost of their own. */
        public int getEnergyCostBase() {
            return this.energyCostBase;
        }

        public int getEnergyCostUpgrade() {
            return this.energyCostUpgrade;
        }

        public int getWorkTicksBase() {
            return this.workTicksBase;
        }

        public int getWorkTicksUpgrade() {
            return this.workTicksUpgrade;
        }
    }
}
