import os
import shutil
import json

base_dir = r"D:\codingan\b-secret"
resources_dir = os.path.join(base_dir, "secretneighbor_resources")

# 1. Create target directories
block_throwable_dir = os.path.join(resources_dir, "assets", "minecraft", "textures", "block", "throwable")
block_materials_dir = os.path.join(resources_dir, "assets", "minecraft", "textures", "block", "materials")
os.makedirs(block_throwable_dir, exist_ok=True)
os.makedirs(block_materials_dir, exist_ok=True)

# 2. Move files from item/throwable/ to block/throwable/
item_throwable_dir = os.path.join(resources_dir, "assets", "minecraft", "textures", "item", "throwable")
if os.path.exists(item_throwable_dir):
    for file in os.listdir(item_throwable_dir):
        src_file = os.path.join(item_throwable_dir, file)
        dst_file = os.path.join(block_throwable_dir, file)
        shutil.move(src_file, dst_file)
        print(f"Moved texture: {file} to block/throwable/")
    
    # Remove empty directory
    os.rmdir(item_throwable_dir)
    print("Removed empty folder: item/throwable/")

# 3. Move neighbor_hd.png from item/ to block/materials/
neighbor_hd_src = os.path.join(resources_dir, "assets", "minecraft", "textures", "item", "neighbor_hd.png")
neighbor_hd_dst = os.path.join(block_materials_dir, "neighbor_hd.png")
if os.path.exists(neighbor_hd_src):
    shutil.move(neighbor_hd_src, neighbor_hd_dst)
    print("Moved neighbor_hd.png to block/materials/")

# 4. Update copy_textures.py dst_dir
copy_textures_py = os.path.join(base_dir, "copy_textures.py")
if os.path.exists(copy_textures_py):
    with open(copy_textures_py, "r") as f:
        content = f.read()
    
    # Replace the destination directory to point to block instead of item
    old_dst = r"secretneighbor_resources\assets\minecraft\textures\item\throwable"
    new_dst = r"secretneighbor_resources\assets\minecraft\textures\block\throwable"
    content = content.replace(old_dst, new_dst)
    
    # Also double check absolute path variant
    old_dst_abs = r"textures\item\throwable"
    new_dst_abs = r"textures\block\throwable"
    content = content.replace(old_dst_abs, new_dst_abs)
    
    with open(copy_textures_py, "w") as f:
        f.write(content)
    print("Updated copy_textures.py destination path.")

# 5. Update JSON models
models_dir = os.path.join(resources_dir, "assets", "minecraft", "models", "item")
for root, _, files in os.walk(models_dir):
    for file in files:
        if file.endswith(".json"):
            path = os.path.join(root, file)
            try:
                with open(path, "r") as f:
                    data = json.load(f)
                
                modified = False
                if "textures" in data:
                    for key, val in data["textures"].items():
                        if isinstance(val, str):
                            # Replace item/throwable with block/throwable
                            if "item/throwable/" in val:
                                val = val.replace("item/throwable/", "block/throwable/")
                                data["textures"][key] = val
                                modified = True
                            # Replace item/neighbor_hd with block/materials/neighbor_hd
                            if "item/neighbor_hd" in val:
                                val = val.replace("item/neighbor_hd", "block/materials/neighbor_hd")
                                data["textures"][key] = val
                                modified = True
                                
                if modified:
                    with open(path, "w") as f:
                        json.dump(data, f, indent=2)
                    print(f"Updated texture references in model: {file}")
            except Exception as e:
                print(f"Error processing model {file}: {e}")

print("\nMigration completed successfully!")
