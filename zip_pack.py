import zipfile
import os

base_dir = os.path.dirname(os.path.abspath(__file__))

# 1. Zip resource pack
zip_path = os.path.join(base_dir, "src", "main", "resources", "resourcepack.zip")
source_dir = os.path.join(base_dir, "secretneighbor_resources")

os.makedirs(os.path.dirname(zip_path), exist_ok=True)

with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, source_dir)
            archive_name = rel_path.replace(os.path.sep, '/')
            zipf.write(file_path, archive_name)

print("Successfully zipped Secret Neighbor resource pack!")

# 2. Zip map folder
map_zip_path = os.path.join(base_dir, "src", "main", "resources", "Secret_Neighbor_Map_2.zip")
map_source_dir = os.path.join(base_dir, "secret_neighbor_map_2")

if os.path.exists(map_source_dir):
    with zipfile.ZipFile(map_zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(map_source_dir):
            # Prune session-specific, player-specific or entity directories
            dirs[:] = [d for d in dirs if d not in ["entities", "playerdata", "stats", "poi", "advancements"]]
            for file in files:
                if file == "session.lock":
                    continue
                file_path = os.path.join(root, file)
                rel_path = os.path.relpath(file_path, map_source_dir)
                archive_name = rel_path.replace(os.path.sep, '/')
                zipf.write(file_path, archive_name)
    print("Successfully zipped Secret Neighbor map!")
else:
    print(f"Warning: Map source directory {map_source_dir} not found. Skipping zipping.")
