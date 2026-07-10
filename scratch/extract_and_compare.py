import os
import zipfile
import filecmp
import shutil

def compare_dirs(dir1, dir2):
    dcmp = filecmp.dircmp(dir1, dir2)
    if dcmp.left_only:
        print(f"Files only in {dir1}: {dcmp.left_only}")
    if dcmp.right_only:
        print(f"Files only in {dir2}: {dcmp.right_only}")
    if dcmp.diff_files:
        print(f"Different files: {dcmp.diff_files}")
    for sub_dir in dcmp.common_dirs:
        compare_dirs(os.path.join(dir1, sub_dir), os.path.join(dir2, sub_dir))

if __name__ == "__main__":
    zip_path = r"d:\codingan\a-secret\src\main\resources\resourcepack.zip"
    extract_path = r"d:\codingan\b-secret\scratch\extracted_a"
    
    if os.path.exists(extract_path):
        shutil.rmtree(extract_path)
    os.makedirs(extract_path, exist_ok=True)
    
    print(f"Extracting {zip_path} to {extract_path}...")
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(extract_path)
        
    dir2 = r"d:\codingan\b-secret\secretneighbor_resources"
    print(f"Comparing:\n  {extract_path}\n  {dir2}")
    compare_dirs(extract_path, dir2)
    print("Done comparing.")
