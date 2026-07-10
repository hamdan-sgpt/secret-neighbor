import os
import shutil
from PIL import Image
import colorsys

base_dir = r"D:\codingan\b-secret"

# Paths
src_door_bottom = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "iron_door_bottom.png")
src_door_top = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "iron_door_top.png")

src_copper_bottom = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "copper_door_bottom.png")
src_copper_top = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "copper_door_top.png")

dest_block_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block")
os.makedirs(dest_block_dir, exist_ok=True)

# 1. Copy iron door (basement escape door)
shutil.copy2(src_door_bottom, os.path.join(dest_block_dir, "iron_door_bottom.png"))
shutil.copy2(src_door_top, os.path.join(dest_block_dir, "iron_door_top.png"))
print("Iron door copied.")

# Helper to change red light to another color in copper door top texture
def tint_red_light(src_path, dest_path, target_color):
    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    # Get target hue
    tr, tg, tb = target_color
    th, tl, ts = colorsys.rgb_to_hls(tr/255.0, tg/255.0, tb/255.0)

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                r_f, g_f, b_f = r / 255.0, g / 255.0, b / 255.0
                h, l, s = colorsys.rgb_to_hls(r_f, g_f, b_f)
                
                is_red = False
                if s > 0.10 and (h < 0.08 or h > 0.92):
                    is_red = True
                elif r > 100 and r > g + 12 and r > b + 12:
                    is_red = True
                elif r > 180 and g > 80 and b > 80 and r > g + 30 and r > b + 30:
                    is_red = True
                
                if is_red:
                    r_new, g_new, b_new = colorsys.hls_to_rgb(th, l, s)
                    pixels[x, y] = (int(r_new * 255), int(g_new * 255), int(b_new * 255), a)
                    
    img.save(dest_path)

# 2. Process Copper Doors (Keycard Doors)
# Level 1: Blue dot door -> copper_door
shutil.copy2(src_copper_bottom, os.path.join(dest_block_dir, "copper_door_bottom.png"))
tint_red_light(src_copper_top, os.path.join(dest_block_dir, "copper_door_top.png"), (0, 180, 255)) # Cyan/Blue

# Level 2: Yellow dot door -> exposed_copper_door
shutil.copy2(src_copper_bottom, os.path.join(dest_block_dir, "exposed_copper_door_bottom.png"))
tint_red_light(src_copper_top, os.path.join(dest_block_dir, "exposed_copper_door_top.png"), (255, 200, 0)) # Yellow/Gold

# Level 3: Red dot door -> weathered_copper_door
shutil.copy2(src_copper_bottom, os.path.join(dest_block_dir, "weathered_copper_door_bottom.png"))
shutil.copy2(src_copper_top, os.path.join(dest_block_dir, "weathered_copper_door_top.png")) # Keep original Red

# Unlocked: Green dot door -> oxidized_copper_door
shutil.copy2(src_copper_bottom, os.path.join(dest_block_dir, "oxidized_copper_door_bottom.png"))
tint_red_light(src_copper_top, os.path.join(dest_block_dir, "oxidized_copper_door_top.png"), (40, 220, 80)) # Green
print("Copper doors processed (including unlocked/oxidized variants).")

# 3. Process Keycards
src_keycard = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "item", "keycard.png")
dest_item_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "item", "materials")
os.makedirs(dest_item_dir, exist_ok=True)

# Level 1: Cyan (Original)
shutil.copy2(src_keycard, os.path.join(dest_item_dir, "keycard_lvl1.png"))

# Function to shift cyan range (hue 0.40 - 0.72) to a new hue
def generate_shifted_keycard(src_path, dest_path, target_hue):
    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                r_f, g_f, b_f = r / 255.0, g / 255.0, b / 255.0
                h, l, s = colorsys.rgb_to_hls(r_f, g_f, b_f)
                if 0.40 <= h <= 0.72:
                    s_new = min(1.0, s * 1.1)
                    r_new, g_new, b_new = colorsys.hls_to_rgb(target_hue, l, s_new)
                    pixels[x, y] = (int(r_new * 255), int(g_new * 255), int(b_new * 255), a)
    img.save(dest_path)

# Level 2: Gold/Yellow (Hue ~0.09)
generate_shifted_keycard(src_keycard, os.path.join(dest_item_dir, "keycard_lvl2.png"), 0.09)

# Level 3: Red (Hue ~0.0)
generate_shifted_keycard(src_keycard, os.path.join(dest_item_dir, "keycard_lvl3.png"), 0.0)
print("Keycards processed.")
