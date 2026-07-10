import os
from PIL import Image, ImageDraw

base_dir = r"D:\codingan\b-secret"
dest_materials_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "textures", "block", "materials")
os.makedirs(dest_materials_dir, exist_ok=True)

# 1. Generate fridge_body.png (128x128 sleek metallic textures)
def generate_fridge_body():
    img = Image.new("RGBA", (128, 128), (200, 200, 200, 255))
    draw = ImageDraw.Draw(img)
    
    # Create a nice steel gradient
    for y in range(128):
        # Base metallic gradient
        factor = abs(y - 64) / 64.0
        val = int(170 + (30 * (1.0 - factor)))
        draw.line([(0, y), (127, y)], fill=(val, val, val + 5, 255))
        
    # Add subtle steel brush texture (vertical lines)
    for x in range(0, 128, 4):
        draw.line([(x, 0), (x, 127)], fill=(0, 0, 0, 10))
        
    # Draw dark highlights and border
    draw.rectangle([0, 0, 127, 127], outline=(80, 80, 85, 255), width=2)
    
    img.save(os.path.join(dest_materials_dir, "fridge_body.png"))
    print("Generated fridge_body.png")

# 2. Generate fridge_front.png (128x128 realistic refrigerator front with details)
def generate_fridge_front():
    # We will draw a double door design (top freezer, bottom fridge)
    img = Image.new("RGBA", (128, 128), (220, 220, 225, 255))
    draw = ImageDraw.Draw(img)
    
    # Steel gradient
    for y in range(128):
        factor = y / 127.0
        val = int(180 + 35 * factor)
        draw.line([(0, y), (127, y)], fill=(val - 10, val - 5, val + 5, 255))
        
    # Add vertical brush strokes
    for x in range(1, 127, 2):
        draw.line([(x, 0), (x, 127)], fill=(255, 255, 255, 12 if x % 4 == 0 else 4))
        
    # Double door separator line (freezer / main compartment)
    # Freezer ends at y=48
    draw.line([(0, 48), (127, 48)], fill=(50, 50, 55, 255), width=2)
    
    # Display panel on freezer door (e.g. ice maker or temperature panel)
    # y range: 16 to 36, x range: 48 to 80
    draw.rectangle([44, 12, 84, 38], fill=(30, 30, 35, 255), outline=(70, 70, 75, 255), width=2)
    # Blue LED screen light inside the panel
    draw.rectangle([48, 16, 80, 24], fill=(0, 140, 255, 255))
    # LED text representation (white lines)
    draw.line([(52, 20), (62, 20)], fill=(255, 255, 255, 255), width=2)
    draw.line([(68, 20), (76, 20)], fill=(255, 255, 255, 255), width=2)
    
    # Dark shadow/borders around the front panel
    draw.rectangle([0, 0, 127, 127], outline=(60, 60, 65, 255), width=2)
    
    img.save(os.path.join(dest_materials_dir, "fridge_front.png"))
    print("Generated fridge_front.png")

# 3. Generate fridge_handle.png (32x128 shiny steel handle)
def generate_fridge_handle():
    img = Image.new("RGBA", (32, 128), (120, 120, 125, 255))
    draw = ImageDraw.Draw(img)
    
    # Metal handle horizontal gradient (vertical cylinder look)
    for x in range(32):
        factor = abs(x - 16) / 16.0
        val = int(80 + (130 * (1.0 - factor)))
        draw.line([(x, 0), (x, 127)], fill=(val, val, val + 10, 255))
        
    # Dark border
    draw.rectangle([0, 0, 31, 127], outline=(40, 40, 45, 255), width=1)
    
    img.save(os.path.join(dest_materials_dir, "fridge_handle.png"))
    print("Generated fridge_handle.png")

if __name__ == "__main__":
    generate_fridge_body()
    generate_fridge_front()
    generate_fridge_handle()
