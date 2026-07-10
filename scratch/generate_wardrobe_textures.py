import os
from PIL import Image, ImageDraw

base_dir = r"D:\codingan\b-secret"
dest_materials_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "materials")
os.makedirs(dest_materials_dir, exist_ok=True)

# 1. Generate wardrobe_wood.png (128x128 rich wood texture)
def generate_wardrobe_wood():
    img = Image.new("RGBA", (128, 128), (101, 67, 33, 255)) # Dark walnut wood base
    draw = ImageDraw.Draw(img)
    
    # Wooden planks gradient
    for y in range(128):
        factor = abs(y - 64) / 64.0
        r = int(115 - (20 * factor))
        g = int(77 - (15 * factor))
        b = int(38 - (10 * factor))
        draw.line([(0, y), (127, y)], fill=(r, g, b, 255))
        
    # Vertical wood grain lines
    for x in range(0, 128, 8):
        draw.line([(x, 0), (x, 127)], fill=(65, 40, 15, 40))
        
    # Borders
    draw.rectangle([0, 0, 127, 127], outline=(50, 30, 10, 255), width=2)
    
    img.save(os.path.join(dest_materials_dir, "wardrobe_wood.png"))
    print("Generated wardrobe_wood.png")

# 2. Generate wardrobe_door.png (128x128 classic wooden wardrobe door)
def generate_wardrobe_door():
    # Double door panel style
    img = Image.new("RGBA", (128, 128), (139, 90, 43, 255)) # Warm cherry wood
    draw = ImageDraw.Draw(img)
    
    # Draw double panels (left door, right door)
    # Left door: x from 2 to 62
    # Right door: x from 65 to 125
    draw.rectangle([2, 2, 62, 125], fill=(120, 75, 35, 255), outline=(75, 45, 20, 255), width=3)
    draw.rectangle([65, 2, 125, 125], fill=(120, 75, 35, 255), outline=(75, 45, 20, 255), width=3)
    
    # Add inner panel details for realism
    draw.rectangle([10, 10, 54, 55], fill=(100, 60, 25, 255), outline=(60, 35, 15, 255), width=2)
    draw.rectangle([10, 68, 54, 115], fill=(100, 60, 25, 255), outline=(60, 35, 15, 255), width=2)
    
    draw.rectangle([73, 10, 117, 55], fill=(100, 60, 25, 255), outline=(60, 35, 15, 255), width=2)
    draw.rectangle([73, 68, 117, 115], fill=(100, 60, 25, 255), outline=(60, 35, 15, 255), width=2)
    
    # Door separator line shadow
    draw.line([(63, 0), (63, 127)], fill=(40, 20, 5, 200), width=1)
    draw.line([(64, 0), (64, 127)], fill=(80, 50, 20, 150), width=1)
    
    # Frame highlight
    draw.rectangle([0, 0, 127, 127], outline=(55, 35, 15, 255), width=2)
    
    img.save(os.path.join(dest_materials_dir, "wardrobe_door.png"))
    print("Generated wardrobe_door.png")

# 3. Generate wardrobe_handle.png (32x128 shiny brass/gold handle)
def generate_wardrobe_handle():
    img = Image.new("RGBA", (32, 128), (218, 165, 32, 255)) # Golden rod brass
    draw = ImageDraw.Draw(img)
    
    # Cylinder shine effect
    for x in range(32):
        factor = abs(x - 16) / 16.0
        val_r = int(180 + (75 * (1.0 - factor)))
        val_g = int(140 + (70 * (1.0 - factor)))
        val_b = int(30 + (50 * (1.0 - factor)))
        draw.line([(x, 0), (x, 127)], fill=(val_r, val_g, val_b, 255))
        
    draw.rectangle([0, 0, 31, 127], outline=(100, 70, 10, 255), width=1)
    
    img.save(os.path.join(dest_materials_dir, "wardrobe_handle.png"))
    print("Generated wardrobe_handle.png")

if __name__ == "__main__":
    generate_wardrobe_wood()
    generate_wardrobe_door()
    generate_wardrobe_handle()
