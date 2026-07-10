from PIL import Image

def process():
    with Image.open(r"D:\codingan\b-secret\hello-neighbor-hd-skin-repaired.png") as img:
        resized = img.resize((64, 64), Image.Resampling.LANCZOS)
        resized.save(r"D:\codingan\b-secret\skin_resized.png", format='PNG')
    print("Resized to skin_resized.png")

if __name__ == "__main__":
    process()
