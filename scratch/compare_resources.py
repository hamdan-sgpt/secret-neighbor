import os
import filecmp

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
    dir1 = r"d:\codingan\exit8_aseli\exit8_resources"
    dir2 = r"d:\codingan\b-secret\exit8_aseli_rill\exit8_resources"
    print(f"Comparing:\n  {dir1}\n  {dir2}")
    if os.path.exists(dir1) and os.path.exists(dir2):
        compare_dirs(dir1, dir2)
    else:
        print("One of the directories does not exist!")
    print("Done comparing.")
