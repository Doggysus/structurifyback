package com.faboslav.structurify.common;

import com.faboslav.structurify.common.api.StructurifyRandomSpreadStructurePlacement;
import com.faboslav.structurify.common.api.StructurifyStructure;
import com.faboslav.structurify.common.config.StructurifyConfig;
import com.faboslav.structurify.common.events.common.LoadConfigEvent;
import com.faboslav.structurify.common.events.common.PrepareRegistriesEvent;
import com.faboslav.structurify.common.modcompat.ModChecker;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public final class Structurify {
    public static final String MOD_ID = "structurify";
    private static final Logger LOGGER = LoggerFactory.getLogger(Structurify.MOD_ID);
    private static final StructurifyConfig CONFIG = new StructurifyConfig();

    public static String makeStringID(String name) {
        return MOD_ID + ":" + name;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static StructurifyConfig getConfig() {
        return CONFIG;
    }

    public static Identifier makeId(String path) {
        /*? >=1.21 {*/
		/*return Identifier.of(
			MOD_ID,
			path
		);
		 *//*?} else {*/
        return new Identifier(
                MOD_ID,
                path
        );
        /*?}*/
    }

    public static Identifier makeVanillaId(String id) {
        /*? >=1.21 {*/
		/*return Identifier.of(
			id
		);
		*//*?} else {*/
        return new Identifier(
                id
        );
        /*?}*/
    }

    public static void init() {
        Structurify.getConfig().create();
        ModChecker.setupModCompat();

        LoadConfigEvent.EVENT.addListener(Structurify::loadConfig);
        PrepareRegistriesEvent.EVENT.addListener(Structurify::prepareRegistries);
        // DatapackReloadEvent.EVENT.addListener(Structurify::reloadStructurifyRegistryManager);
    }

    private static void loadConfig(final LoadConfigEvent event) {
        if (Structurify.getConfig().isLoaded) {
            return;
        }

        Structurify.getConfig().load();

        List<String> disabledStructures = Structurify.getConfig().getStructureData().entrySet()
                .stream()
                .filter(entry -> entry.getValue().isDisabled())
                .map(Map.Entry::getKey)
                .toList();

        if (!disabledStructures.isEmpty()) {
            Structurify.getLogger().info("Disabled {} structures: {}", disabledStructures.size(), disabledStructures);
        }

        List<String> changedStructureSets = Structurify.getConfig().getStructureSetData().entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isUsingDefaultSpacingAndSeparation())
                .map(Map.Entry::getKey)
                .toList();

        if (Structurify.getConfig().enableGlobalSpacingAndSeparationModifier && Structurify.getConfig().globalSpacingAndSeparationModifier != 1.0D) {
            Structurify.getLogger().info("Enabled global spacing and separation modifier with value of {}", Structurify.getConfig().globalSpacingAndSeparationModifier);
        }

        if (!changedStructureSets.isEmpty()) {
            Structurify.getLogger().info("Changed spacing and/or separation of {} structures sets: {}", changedStructureSets.size(), changedStructureSets);
        }
    }

	/*
	private static void reloadStructurifyRegistryManager(final DatapackReloadEvent event) {
		WorldgenDataProvider.reload();
	} */

    private static void prepareRegistries(final PrepareRegistriesEvent event) {
        if (!Structurify.getConfig().isLoaded) {
            return;
        }

        var registryManager = event.registryManager();

        if (registryManager == null) {
            return;
        }

        Structurify.prepareStructures(registryManager);
        Structurify.prepareStructureSets(registryManager);
    }

    private static void prepareStructures(DynamicRegistryManager registryManager) {
        var structureRegistry = registryManager.getOptional(RegistryKeys.STRUCTURE).orElse(null);

        if (structureRegistry == null) {
            return;
        }

        for (var structure : structureRegistry) {
            RegistryKey<Structure> structureRegistryKey = structureRegistry.getKey(structure).orElse(null);

            if (structureRegistryKey == null) {
                continue;
            }

            Identifier structureId = structureRegistryKey.getValue();
            ((StructurifyStructure) structure).structurify$setStructureIdentifier(structureId);
        }
    }

    private static void prepareStructureSets(DynamicRegistryManager registryManager) {
        var structureSetRegistry = registryManager.getOptional(RegistryKeys.STRUCTURE_SET).orElse(null);

        if (structureSetRegistry == null) {
            return;
        }

        for (StructureSet structureSet : structureSetRegistry) {
            RegistryKey<StructureSet> structureSetRegistryKey = structureSetRegistry.getKey(structureSet).orElse(null);

            if (structureSetRegistryKey == null) {
                continue;
            }

            Identifier structureSetId = structureSetRegistryKey.getValue();

            if (structureSet.placement() instanceof net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement randomSpreadStructurePlacement) {
                ((StructurifyRandomSpreadStructurePlacement) randomSpreadStructurePlacement).structurify$setStructureSetIdentifier(structureSetId);
            }
        }
    }
}
