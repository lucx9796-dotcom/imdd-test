package br.com.irmandade.imdd.tab;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/** Converte texto com códigos &0-&f, &l &o &n &m &k &r e cores hex &#RRGGBB em Text. */
public final class TabText {
    private static final int[] COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private TabText() {}

    public static MutableText parse(String in) {
        MutableText root = Text.empty();
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;
        int i = 0;
        while (i < in.length()) {
            char c = in.charAt(i);
            if (c == '&' && i + 1 < in.length()) {
                char n = Character.toLowerCase(in.charAt(i + 1));
                if (n == '#' && i + 8 <= in.length() && isHex(in.substring(i + 2, i + 8))) {
                    flush(root, buf, style);
                    style = Style.EMPTY.withColor(Integer.parseInt(in.substring(i + 2, i + 8), 16));
                    i += 8;
                    continue;
                }
                int idx = "0123456789abcdef".indexOf(n);
                if (idx >= 0) {
                    flush(root, buf, style);
                    style = Style.EMPTY.withColor(COLORS[idx]);
                    i += 2;
                    continue;
                }
                if ("lonmkr".indexOf(n) >= 0) {
                    flush(root, buf, style);
                    switch (n) {
                        case 'l' -> style = style.withBold(true);
                        case 'o' -> style = style.withItalic(true);
                        case 'n' -> style = style.withUnderline(true);
                        case 'm' -> style = style.withStrikethrough(true);
                        case 'k' -> style = style.withObfuscated(true);
                        default -> style = Style.EMPTY;
                    }
                    i += 2;
                    continue;
                }
            }
            buf.append(c);
            i++;
        }
        flush(root, buf, style);
        return root;
    }

    /** Largura aproximada em pixels na fonte padrão (negrito soma +1 por caractere). */
    public static int width(String in) {
        int w = 0;
        boolean bold = false;
        int i = 0;
        while (i < in.length()) {
            char c = in.charAt(i);
            if (c == '&' && i + 1 < in.length()) {
                char n = Character.toLowerCase(in.charAt(i + 1));
                if (n == '#' && i + 8 <= in.length() && isHex(in.substring(i + 2, i + 8))) { bold = false; i += 8; continue; }
                if ("0123456789abcdef".indexOf(n) >= 0) { bold = false; i += 2; continue; }
                if ("lonmkr".indexOf(n) >= 0) {
                    if (n == 'l') bold = true;
                    else if (n == 'r') bold = false;
                    i += 2;
                    continue;
                }
            }
            w += charWidth(c) + (bold ? 1 : 0);
            i++;
        }
        return w;
    }

    private static int charWidth(char c) {
        switch (c) {
            case ' ': return 4;
            case '!': case ',': case '.': case ':': case ';': case '|': case 'i': return 2;
            case 39: case '`': case 'l': return 3; // 39 = aspa simples
            case 'I': case '[': case ']': case 't': return 4;
            case '"': case '(': case ')': case '*': case '<': case '>': case 'f': case 'k': case '{': case '}': return 5;
            case '@': case '~': return 7;
            default: return 6;
        }
    }

    private static void flush(MutableText root, StringBuilder buf, Style style) {
        if (buf.length() == 0) return;
        root.append(Text.literal(buf.toString()).setStyle(style));
        buf.setLength(0);
    }

    private static boolean isHex(String s) {
        for (char ch : s.toCharArray()) if (Character.digit(ch, 16) < 0) return false;
        return true;
    }
}
