package com.yuan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MixinConfigCheck {
    private static final Pattern MIXIN_NAME = Pattern.compile("\"([A-Za-z0-9_]+Mixin)\"");

    public static void main(String[] args) throws Exception {
        String config = Files.readString(Path.of("src/main/resources/yuan.mixins.json"));
        assert config.contains("ItemRendererMixin") : "cosmic item renderer mixin must be registered";
        assert !config.contains("UtilMixin") : "UtilMixin stays unregistered on purpose";

        // A registered name without a source file is a boot-time apply failure, which no compile
        // step catches. The reverse (a file with no registration) is intentional for the legacy
        // yuan_sword freeze chain and is deliberately not asserted here.
        Set<String> registered = registeredMixins(config);
        assert registered.size() >= 50
                : "the freeze chain must stay registered, only found " + registered.size() + " entries";
        for (String name : registered) {
            assert Files.exists(Path.of("src/main/java/com/yuan/mixin/" + name + ".java"))
                    : name + " is registered in yuan.mixins.json but has no source file";
        }

        String swordEvents = Files.readString(Path.of(
                "src/main/java/com/yuan/event/YuanSwordEvents.java"));
        assert swordEvents.contains("if (player.getInventory() == null) return ItemStack.EMPTY;")
                : "defense lookup must tolerate Player construction before inventory initialization";

        System.out.println("MixinConfigCheck OK");
    }

    private static Set<String> registeredMixins(String config) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = MIXIN_NAME.matcher(config);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
