import os
from PIL import Image

def resize_images(directories):
    for directory in directories:
        for root, _, files in os.walk(directory):
            for file in files:
                if file.lower().endswith(".png"):
                    path = os.path.join(root, file)
                    try:
                        with Image.open(path) as img:
                            w, h = img.size
                            if w == 128 and h == 128:
                                continue
                            
                            # Decide interpolation
                            if w < 128 or h < 128:
                                # Upscaling small images (e.g. 16x16) -> use Nearest Neighbor to keep pixel art sharp
                                resample = Image.Resampling.NEAREST
                            else:
                                # Downscaling large images (e.g. 1024x1024) -> use Lanczos for best quality
                                resample = Image.Resampling.LANCZOS

                            resized = img.resize((128, 128), resample)
                        
                        # Save it back
                        resized.save(path)
                        print(f"Resized {file} from {w}x{h} to 128x128")
                    except Exception as e:
                        print(f"Error processing {path}: {e}")

if __name__ == "__main__":
    target_dirs = [
        r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\item",
        r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\textures\block"
    ]
    resize_images(target_dirs)
