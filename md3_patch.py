import os, re, shutil, sys

J = "TMessagesProj/src/main/java/"
ok = True

def rd(p):
    with open(p, encoding="utf-8") as f:
        return f.read()

def wr(p, s):
    with open(p, "w", encoding="utf-8") as f:
        f.write(s)

def sub(path, pattern, repl, marker, name):
    global ok
    s = rd(path)
    if marker in s:
        print("[skip] " + name)
        return
    s2, n = re.subn(pattern, lambda m: repl(m), s, count=1)
    if n == 0:
        print("::error::MD3 patch failed: " + name)
        ok = False
        return
    wr(path, s2)
    print("[ok] " + name)

cfg = J + "com/th3nekit/finegram/core/configs/FinegramAppearanceConfig.kt"
sub(cfg,
    r'(\n([ \t]*)var marketPlaceDrawerButton by sharedPreferences\.boolean\("AP_MarketplaceDrawerButton", true\)[^\n]*)',
    lambda m: m.group(1) + "\n" + m.group(2) + 'var md3Loader by sharedPreferences.boolean("FG_MD3Loader", true)\n'
              + m.group(2) + 'var md3Font by sharedPreferences.boolean("FG_MD3Font", true)',
    "FG_MD3Loader", "config")

rpv = J + "org/telegram/ui/Components/RadialProgressView.java"
sub(rpv,
    r'public static boolean MD3_LOADER\s*=\s*true\s*;',
    lambda m: "public static boolean MD3_LOADER = com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Loader();",
    "getMd3Loader()", "loader toggle")

au = J + "org/telegram/messenger/AndroidUtilities.java"
sub(au,
    r'if \(SharedConfig\.useSystemBoldFont && Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.P\)',
    lambda m: "if (SharedConfig.useSystemBoldFont && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Font())",
    "VERSION_CODES.P && !com.th3nekit", "bold font")
sub(au,
    r'Typeface t = com\.exteragram\.messenger\.utils\.ui\.FontUtils\.getSystemTypeface\(assetPath\);',
    lambda m: "Typeface t = md3Typeface(assetPath);\n                if (t == null) {\n                    t = com.exteragram.messenger.utils.ui.FontUtils.getSystemTypeface(assetPath);\n                }",
    "Typeface t = md3Typeface(assetPath);", "typeface hook")
MD3_METHOD = r'''    private static Typeface md3Typeface(String assetPath) {
        if (Build.VERSION.SDK_INT < 26) return null;
        try {
            if (!com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Font()) return null;
            int weight;
            if (TYPEFACE_ROBOTO_REGULAR.equals(assetPath) || TYPEFACE_ROBOTO_ITALIC.equals(assetPath)) {
                weight = 400;
            } else if (TYPEFACE_ROBOTO_MEDIUM.equals(assetPath) || TYPEFACE_ROBOTO_MEDIUM_ITALIC.equals(assetPath)) {
                weight = 600;
            } else if (TYPEFACE_ROBOTO_EXTRA_BOLD.equals(assetPath)) {
                weight = 800;
            } else {
                return null;
            }
            try {
                ApplicationLoader.applicationContext.getAssets().open("fonts/gsans.ttf").close();
            } catch (Throwable missing) {
                return null;
            }
            Typeface.Builder b = new Typeface.Builder(ApplicationLoader.applicationContext.getAssets(), "fonts/gsans.ttf");
            b.setFontVariationSettings("'wght' " + weight);
            b.setWeight(weight);
            b.setItalic(assetPath.contains("italic"));
            return b.build();
        } catch (Throwable e) {
            return null;
        }
    }

'''
sub(au,
    r'\n([ \t]*)public static Typeface getTypeface\(String assetPath\) \{',
    lambda m: "\n" + MD3_METHOD + m.group(1) + "public static Typeface getTypeface(String assetPath) {",
    "private static Typeface md3Typeface(", "typeface method")

ap = J + "com/th3nekit/finegram/preferences/AppearancePreferencesEntry.java"
sub(ap,
    r'(bottomTabsWithDrawerRow = 1600;)',
    lambda m: m.group(1) + "\n    private final int md3LoaderRow = 1601, md3FontRow = 1602;",
    "md3LoaderRow = 1601", "settings ids")
sub(ap,
    r'\n([ \t]*)items\.add\(UItem\.asHeader\(getString\(R\.string\.AP_Header\)\)\);',
    lambda m: "\n" + m.group(1) + 'items.add(UItem.asHeader("Material Design 3"));\n'
              + m.group(1) + 'items.add(SettingsHelper.asSwitchCG(md3LoaderRow, "MD3 анимация загрузки")\n'
              + m.group(1) + '        .setChecked(com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Loader()));\n'
              + m.group(1) + 'items.add(SettingsHelper.asSwitchCG(md3FontRow, "Шрифт Google Sans")\n'
              + m.group(1) + '        .setChecked(com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Font()));\n'
              + m.group(1) + 'items.add(UItem.asShadow(null));\n'
              + m.group(1) + 'items.add(UItem.asHeader(getString(R.string.AP_Header)));',
    'asHeader("Material Design 3")', "settings rows")
sub(ap,
    r'if \(item\.id == centerTitleRow\) \{',
    lambda m: ("if (item.id == md3LoaderRow) {\n"
               "            com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.setMd3Loader(!com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Loader());\n"
               "            org.telegram.ui.Components.RadialProgressView.MD3_LOADER = com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Loader();\n"
               "            SettingsHelper.updateCheckState(view, com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Loader());\n"
               "        } else if (item.id == md3FontRow) {\n"
               "            com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.setMd3Font(!com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Font());\n"
               "            SettingsHelper.updateCheckState(view, com.th3nekit.finegram.core.configs.FinegramAppearanceConfig.INSTANCE.getMd3Font());\n"
               "            org.telegram.messenger.AndroidUtilities.clearTypefaceCache();\n"
               "            showRestartBulletin();\n"
               "        } else if (item.id == centerTitleRow) {"),
    "item.id == md3LoaderRow", "settings clicks")

dst = "TMessagesProj/src/main/assets/fonts/gsans.ttf"
if os.path.exists("gsans.ttf"):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile("gsans.ttf", dst)
    print("[ok] font copied")
elif os.path.exists(dst):
    print("[ok] font already in assets")
else:
    print("::warning::gsans.ttf not found, Roboto will be used")

if not ok:
    sys.exit(1)
