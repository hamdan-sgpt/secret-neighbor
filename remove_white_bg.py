import os
from PIL import Image
import glob

# Target directory
dst_dir = r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\block\throwable"

def make_white_transparent(img_path):
    with Image.open(img_path) as img:
        img = img.convert("RGBA")
        datas = img.getdata()
        
        newData = []
        # tolerance for "white"
        for item in datas:
            if item[0] > 230 and item[1] > 230 and item[2] > 230:
                newData.append((255, 255, 255, 0))
            else:
                newData.append(item)
                
        img.putdata(newData)
        img.save(img_path, "PNG")

png_files = glob.glob(os.path.join(dst_dir, "*.png"))
for f in png_files:
    make_white_transparent(f)
    print(f"Processed: {os.path.basename(f)}")

print("Done making white backgrounds transparent!")
