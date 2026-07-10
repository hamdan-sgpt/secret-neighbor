import sys
sys.path.append(r"C:\Users\Pongo\AppData\Local\Packages\PythonSoftwareFoundation.Python.3.13_qbz5n2kfra8p0\LocalCache\local-packages\Python313\site-packages")

from PIL import Image

try:
    img_bottom = Image.open(r"d:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\block\iron_door_bottom.png")
    img_top = Image.open(r"d:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\block\iron_door_top.png")
    print(f"Bottom Image size: {img_bottom.size}")
    print(f"Top Image size: {img_top.size}")
except Exception as e:
    print(f"Error: {e}")
