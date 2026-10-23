"""wallpaper_crossfade: the switch that fades the next rotating picture over the current one.

Rotating wallpapers used to swap in one frame. Two 1080p pictures overlapping for about a second
looks calmer in a living room, then the extra bitmap is released. Off by default so existing
installs keep the hard cut; a theme or a fork's config.xml can turn it on. reduced_motion still cuts.

The flag only works if every hop exists: config.xml defines it off, Config reads it and lets a theme
override it, WallpaperPlayer applies it, and the theme table lists it.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


def read(relative):
    """Text of a repo file, or empty if it does not exist yet (each test then fails with its own message)."""
    path = ROOT / relative
    return path.read_text(encoding="utf-8") if path.is_file() else ""


CONFIG_XML = read("launcher/app/src/main/res/values/config.xml")
CONFIG_JAVA = read("launcher/app/src/main/java/com/gathin/tvhome/Config.java")
PLAYER = read("launcher/app/src/main/java/com/gathin/tvhome/WallpaperPlayer.java")
THEMES_README = read("themes/README.md")


class TestWallpaperCrossfadeWiring(unittest.TestCase):

    def test_config_xml_defines_it_off(self):
        self.assertRegex(CONFIG_XML, r'<bool name="wallpaper_crossfade">false</bool>',
                         "config.xml must define wallpaper_crossfade off so existing installs are unchanged")

    def test_config_reads_resource_and_theme_override(self):
        self.assertIn("R.bool.wallpaper_crossfade", CONFIG_JAVA,
                      "Config must read the wallpaper_crossfade resource")
        self.assertIn('"wallpaper_crossfade"', CONFIG_JAVA,
                      'Config must accept a theme config.json "wallpaper_crossfade" override')
        self.assertIn("this.wallpaperCrossfade = wallpaperCrossfade", CONFIG_JAVA,
                      "Config must store wallpaperCrossfade")

    def test_player_applies_it_and_respects_reduced_motion(self):
        self.assertIn("config.wallpaperCrossfade && !config.reducedMotion", PLAYER,
                      "WallpaperPlayer must fade only when the switch is on and reduced_motion is off")
        self.assertIn("show(bitmap, fadeChanges)", PLAYER,
                      "later rotations must pass the fade flag into show")
        self.assertIn("show(bitmap, false)", PLAYER,
                      "Home's first picture must still appear without a fade")

    def test_theme_table_lists_it_off_by_default(self):
        row = next((line for line in THEMES_README.splitlines() if line.startswith("| `wallpaper_crossfade`")), "")
        self.assertTrue(row, "themes/README.md's Keys table must list wallpaper_crossfade")
        self.assertIn("`false` by default", row, "the table must say it is off by default")


if __name__ == "__main__":
    unittest.main()
