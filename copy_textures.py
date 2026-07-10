from PIL import Image
import os
import glob

# Source directory (generated textures)
src_dir = r"C:\Users\Pongo\.gemini\antigravity-ide\brain\c7c5126c-11d9-4b94-bbaa-7f77192630ea"
# Target directory
dst_dir = r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\block\throwable"

# Map generated filenames to target filenames
texture_map = {
    "basketball_texture_": "basketball.png",
    "tomato_texture_": "tomato.png",
    "tomato_leaf_texture_": "tomato_leaf.png",
    "cardboard_box_texture_": "box.png",
    "tv_screen_texture_": "tv_screen.png",
    "tv_casing_texture_": "tv_casing.png",
    "painting_canvas_texture_": "painting_canvas.png",
    "pillow_texture_": "pillow.png",
    "hat_texture_": "hat.png",
    "book_cover_texture_": "book_cover.png",
    "book_pages_": "book_pages.png",
    "chair_texture_": "chair.png",
    "sofa_texture_": "sofa.png",
    "broom_stick_texture_": "broom_stick.png",
    "broom_bristles_texture_": "broom_bristles.png",
}

os.makedirs(dst_dir, exist_ok=True)

for prefix, target_name in texture_map.items():
    # Find the file with this prefix
    matches = glob.glob(os.path.join(src_dir, prefix + "*.png"))
    if not matches:
        print(f"WARNING: No file found for prefix '{prefix}'")
        continue
    
    src_file = matches[0]
    dst_file = os.path.join(dst_dir, target_name)
    
    with Image.open(src_file) as img:
        # Resize to exactly 128x128 using NEAREST for pixelated Minecraft style
        resized = img.resize((128, 128), Image.Resampling.NEAREST)
        # Convert to RGBA if not already
        if resized.mode != "RGBA":
            resized = resized.convert("RGBA")
        resized.save(dst_file, format="PNG")
        print(f"OK: {os.path.basename(src_file)} -> {target_name} (128x128)")

print("\nAll textures processed!")
