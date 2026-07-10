import zipfile

def list_all_files(zip_path):
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        names = zip_ref.namelist()
        print(f"Total files: {len(names)}")
        for name in sorted(names):
            print(f"  {name}")

if __name__ == "__main__":
    list_all_files(r"d:\codingan\a-secret\src\main\resources\resourcepack.zip")
