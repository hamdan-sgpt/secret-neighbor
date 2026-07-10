import zipfile
import os

base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
zip_path = os.path.join(base_dir, "src", "main", "resources", "Secret_Neighbor_Map_2.zip")
source_dir = os.path.join(base_dir, "secret_neighbor_map_2_new")

print(f"Base dir: {base_dir}")
print(f"Zip path: {zip_path}")
print(f"Source dir: {source_dir}")

os.makedirs(os.path.dirname(zip_path), exist_ok=True)

if os.path.exists(zip_path):
    print("Removing old zip file...")
    os.remove(zip_path)

print("Zipping world files...")
file_count = 0
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            if file == "session.lock":
                continue
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, source_dir)
            archive_name = rel_path.replace(os.path.sep, '/')
            zipf.write(file_path, archive_name)
            file_count += 1

print(f"Successfully zipped {file_count} files to {zip_path}!")
