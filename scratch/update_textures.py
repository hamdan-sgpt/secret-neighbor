import os
import shutil
from PIL import Image
import colorsys

base_dir = r"D:\codingan\b-secret"

# 1. Copy iron door textures
src_door_bottom = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "iron_door_bottom.png")
src_door_top = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "iron_door_top.png")

dest_door_bottom = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "iron_door_bottom.png")
dest_door_top = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "iron_door_top.png")

print("Copying iron door textures...")
shutil.copy2(src_door_bottom, dest_door_bottom)
shutil.copy2(src_door_top, dest_door_top)
print("Iron door textures copied successfully!")

# 2. Copy copper door (keycard door) textures
src_copper_bottom = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "copper_door_bottom.png")
src_copper_top = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "block", "copper_door_top.png")

dest_copper_bottom = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "copper_door_bottom.png")
dest_copper_top = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "copper_door_top.png")

print("Copying copper door (keycard door) textures...")
shutil.copy2(src_copper_bottom, dest_copper_bottom)
shutil.copy2(src_copper_top, dest_copper_top)
print("Copper door textures copied successfully!")

# 3. Process Keycards
src_keycard = os.path.join(base_dir, "scratch", "extracted_a", "assets", "minecraft", "textures", "item", "keycard.png")
dest_lvl1 = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "item", "materials", "keycard_lvl1.png")
dest_lvl2 = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "item", "materials", "keycard_lvl2.png")

print("Saving Level 1 Keycard (Cyan)...")
shutil.copy2(src_keycard, dest_lvl1)

print("Generating Level 2 Keycard (Gold/Yellow)...")
img = Image.open(src_keycard).convert("RGBA")
pixels = img.load()
width, height = img.size

for y in range(height):
    for x in range(width):
        r, g, b, a = pixels[x, y]
        if a > 0:
            r_f, g_f, b_f = r / 255.0, g / 255.0, b / 255.0
            h, l, s = colorsys.rgb_to_hls(r_f, g_f, b_f)
            
            if 0.40 <= h <= 0.72:
                h_new = 0.09
                s_new = min(1.0, s * 1.1)
                r_new, g_new, b_new = colorsys.hls_to_rgb(h_new, l, s_new)
                pixels[x, y] = (int(r_new * 255), int(g_new * 255), int(b_new * 255), a)

img.save(dest_lvl2)
print("Level 2 Keycard generated successfully!")
