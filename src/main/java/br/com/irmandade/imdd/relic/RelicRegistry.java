package br.com.irmandade.imdd.relic;

import br.com.irmandade.imdd.Imdd;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As relíquias são definidas com a MESMA sintaxe do /give (1.21.5+), uma por linha: id|item[componentes].
 * A ordem das linhas = ordem dos slots da GUI (slot 1 = primeira linha).
 * Para editar uma relíquia, basta mudar a linha. O id vira custom_data={imdd_relic:"id"}, usado para identificar o item.
 */
public final class RelicRegistry {
    public static final String KEY = "imdd_relic";
    private static final List<ItemStack> STACKS = new ArrayList<>();
    private static final Map<String, ItemStack> BY_ID = new LinkedHashMap<>();

    private static final String DEFINITIONS = """
bizonho|stick[custom_name=["",{"text":"Relíquia do Bizonho","bold":true,"underlined":true,"color":"#b5853f"}],enchantments={knockback:50},unbreakable={},tooltip_display={hidden_components:[attribute_modifiers,unbreakable]}]
fazendeiro|leather_leggings[custom_name=["",{"text":"Relíquia do Fazendeiro","bold":true,"underlined":true,"color":"#037b0d"}],enchantments={protection:8},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
bizonho_machado|diamond_axe[custom_name=["",{"text":"Relíquia do Bizonho","bold":true,"underlined":true,"color":"#b6893f"}],enchantments={knockback:150},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
minerador|netherite_pickaxe[custom_name=["",{"text":"Relíquia do Minerador","bold":true,"underlined":true,"color":"#f9f40f"}],enchantments={efficiency:10,fortune:3},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
cacador|crossbow[custom_name=["",{"text":"Relíquia do Caçador","bold":true,"underlined":true,"color":"#f6024e"}],enchantments={multishot:3,piercing:10,quick_charge:5},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
sorte|rabbit_foot[custom_name=["",{"text":"Relíquia da Sorte","bold":true,"underlined":true,"color":"#07f806"}],enchantment_glint_override=true,unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
guerreiro|netherite_sword[custom_name=["",{"text":"Relíquia do Guerreiro","bold":true,"underlined":true,"color":"#ed020c"}],enchantment_glint_override=true,enchantments={bane_of_arthropods:5,knockback:3,looting:3,sharpness:10,smite:5,sweeping_edge:3},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
defesa|shield[custom_name=["",{"text":"Relíquia da Defesa","bold":true,"underlined":true,"color":"#807b81"}],enchantment_glint_override=true,enchantments={thorns:5},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
fim|elytra[custom_name=["",{"text":"Relíquia do Fim","bold":true,"underlined":true,"color":"#f6faa1"}],enchantment_glint_override=true,enchantments={protection:15},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
invasor|leather_leggings[dyed_color=4212811,custom_name=["",{"text":"Relíquia do Invasor","bold":true,"underlined":true,"color":"#817c7e"}],enchantment_glint_override=true,enchantments={binding_curse:1,protection:8,vanishing_curse:1},unbreakable={}]
imortalidade|totem_of_undying[custom_name=["",{"text":"Relíquia da Imortalidade","bold":true,"underlined":true,"color":"#3fcc7b"}],enchantment_glint_override=true,!death_protection,use_cooldown={seconds:10,cooldown_group:"imdd:imortalidade"},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
oceano|trident[custom_name=["",{"text":"Relíquia do Oceano","bold":true,"underlined":true,"color":"#29eff9"}],enchantment_glint_override=true,enchantments={channeling:1,impaling:10,looting:3,loyalty:3,sharpness:5},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
explorador|diamond_boots[custom_name=["",{"text":"Relíquia do Explorador","bold":true,"underlined":true,"color":"#fcc814"}],enchantment_glint_override=true,enchantments={frost_walker:2},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
espiao|cookie[custom_name=["",{"text":"Relíquia do Espião","bold":true,"underlined":true,"color":"#e09451"}],enchantment_glint_override=true,!food,!consumable,use_cooldown={seconds:1200,cooldown_group:"imdd:espiao"},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
vento|wind_charge[custom_name=["",{"text":"Relíquia do Deus do Vento","bold":true,"underlined":true,"color":"#e9e6f4"}],enchantment_glint_override=true,use_cooldown={seconds:0.3,cooldown_group:"imdd:vento"},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
warden|recovery_compass[custom_name=["",{"text":"Relíquia do Warden","bold":true,"underlined":true,"color":"#13888e"}],enchantment_glint_override=true,use_cooldown={seconds:10,cooldown_group:"imdd:warden"},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
stomper|mace[custom_name=["",{"text":"Relíquia do Stomper","bold":true,"underlined":true,"color":"#302b37"}],enchantment_glint_override=true,enchantments={density:5},unbreakable={},tooltip_display={hidden_components:[enchantments,unbreakable]}]
dragao|dragon_egg[custom_name=["",{"text":"Relíquia do Dragão","bold":true,"underlined":true,"color":"#6a1ef6"}],enchantment_glint_override=true,unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
invisibilidade|leather_boots[custom_name=["",{"text":"Relíquia da Invisibilidade","bold":true,"underlined":true,"color":"white"}],enchantment_glint_override=true,enchantments={protection:10},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
nether|golden_helmet[custom_name=["",{"text":"Relíquia do Nether","bold":true,"underlined":true,"color":"#eda411"}],enchantment_glint_override=true,enchantments={protection:8},unbreakable={},tooltip_display={hidden_components:[unbreakable]}]
""";

    private RelicRegistry() {}

    public static void load(RegistryWrapper.WrapperLookup lookup) {
        STACKS.clear();
        BY_ID.clear();
        ItemStringReader reader = new ItemStringReader(lookup);
        for (String raw : DEFINITIONS.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            int sep = line.indexOf('|');
            String id = line.substring(0, sep);
            String spec = line.substring(sep + 1).replaceFirst("\\[", "[custom_data={" + KEY + ":\"" + id + "\"},");
            try {
                ItemStringReader.ItemResult r = reader.consume(new StringReader(spec));
                ItemStack made = new ItemStack(r.item(), 1, r.components());
                STACKS.add(made);
                BY_ID.put(id, made);
            } catch (Exception e) {
                Imdd.LOGGER.error("Relíquia '{}' inválida, slot ficará vazio: {}", id, e.getMessage());
                STACKS.add(ItemStack.EMPTY);
            }
        }
        Imdd.LOGGER.info("{} relíquias carregadas.", STACKS.size());
    }

    public static List<ItemStack> stacks() { return STACKS; }

    /** Id da relíquia ("" se o item não for uma). */
    public static String idOf(ItemStack stack) {
        if (stack.isEmpty()) return "";
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return "";
        return data.copyNbt().getString(KEY, "");
    }

    /** Ids de todas as relíquias carregadas, na ordem dos slots. */
    public static List<String> ids() { return new ArrayList<>(BY_ID.keySet()); }

    /** Cópia nova da relíquia (ou ItemStack.EMPTY se o id não existir). */
    public static ItemStack copyOf(String id) {
        ItemStack s = BY_ID.get(id);
        return s == null ? ItemStack.EMPTY : s.copy();
    }

    public static boolean isRelic(ItemStack stack) { return !idOf(stack).isEmpty(); }

    public static boolean is(ItemStack stack, String id) { return id.equals(idOf(stack)); }
}
