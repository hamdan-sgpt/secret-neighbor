import os
import random
import math
from PIL import Image, ImageDraw

def add_noise(img, amount=4):
    width, height = img.size
    pixels = img.load()
    for y in range(height):
        for x in range(width):
            r, g, b, *a = pixels[x, y]
            noise = random.randint(-amount, amount)
            r = max(0, min(255, r + noise))
            g = max(0, min(255, g + noise))
            b = max(0, min(255, b + noise))
            if a:
                pixels[x, y] = (r, g, b, a[0])
            else:
                pixels[x, y] = (r, g, b)

def generate_color_panel(path, r_val, g_val, b_val, highlight_r, highlight_g, highlight_b, shadow_r, shadow_g, shadow_b):
    img = Image.new("RGBA", (128, 128), (r_val, g_val, b_val, 255))
    draw = ImageDraw.Draw(img)
    # Highlights & shadows for 3D look
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(highlight_r, highlight_g, highlight_b, 255), width=3)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(shadow_r, shadow_g, shadow_b, 255), width=3)
    add_noise(img, 4)
    img.save(path)

def generate_silver(path):
    img = Image.new("RGBA", (128, 128), (180, 182, 185, 255))
    draw = ImageDraw.Draw(img)
    for _ in range(150):
        x = random.randint(0, 127)
        y = random.randint(0, 127)
        w = random.randint(15, 60)
        draw.line([(x, y), (x + w, y)], fill=(195, 197, 200, 255), width=1)
        draw.line([(x, y + 1), (x + w, y + 1)], fill=(160, 162, 165, 255), width=1)
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(230, 232, 235, 255), width=2)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(130, 132, 135, 255), width=2)
    add_noise(img, 2)
    img.save(path)

def generate_brown_wood(path):
    img = Image.new("RGBA", (128, 128), (145, 95, 55, 255))
    draw = ImageDraw.Draw(img)
    for i in range(8):
        y = i * 16 + random.randint(-4, 4)
        points = []
        for x in range(0, 140, 10):
            points.append((x, y + int(6 * random.random())))
        draw.line(points, fill=(110, 70, 35, 255), width=random.randint(1, 3))
    add_noise(img, 3)
    img.save(path)

def generate_black(path):
    img = Image.new("RGBA", (128, 128), (40, 40, 40, 255))
    draw = ImageDraw.Draw(img)
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(65, 65, 65, 255), width=2)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(20, 20, 20, 255), width=2)
    add_noise(img, 2)
    img.save(path)

def generate_white(path):
    img = Image.new("RGBA", (128, 128), (240, 240, 240, 255))
    draw = ImageDraw.Draw(img)
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(255, 255, 255, 255), width=2)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(210, 210, 210, 255), width=2)
    add_noise(img, 1)
    img.save(path)

def generate_l_yellow(path):
    img = Image.new("RGBA", (128, 128), (255, 220, 30, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 127, 127], outline=(255, 250, 150, 255), width=4)
    draw.rectangle([4, 4, 123, 123], outline=(225, 185, 10, 255), width=3)
    add_noise(img, 2)
    img.save(path)

def generate_dark_brown(path):
    img = Image.new("RGBA", (128, 128), (95, 65, 45, 255))
    draw = ImageDraw.Draw(img)
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(125, 90, 65, 255), width=2)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(65, 40, 25, 255), width=2)
    add_noise(img, 5)
    img.save(path)

def generate_screen(path):
    img = Image.new("RGBA", (128, 128), (20, 45, 25, 255))
    draw = ImageDraw.Draw(img)
    for y in range(0, 128, 4):
        draw.line([(0, y), (127, y)], fill=(25, 55, 30, 255), width=1)
    cx, cy = 64, 64
    draw.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], outline=(30, 75, 40, 255), width=1)
    draw.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], outline=(30, 75, 40, 255), width=1)
    draw.line([(cx, 10), (cx, 118)], fill=(30, 75, 40, 255), width=1)
    draw.line([(10, cy), (118, cy)], fill=(30, 75, 40, 255), width=1)
    wave_points = []
    for x in range(10, 118):
        y = cy + int(15 * math.sin(x * 0.15))
        wave_points.append((x, y))
    draw.line(wave_points, fill=(60, 220, 80, 255), width=2)
    draw.rectangle([0, 0, 127, 127], outline=(10, 25, 15, 255), width=3)
    add_noise(img, 1)
    img.save(path)

def generate_light_red(path):
    img = Image.new("RGBA", (128, 128), (230, 30, 30, 255))
    draw = ImageDraw.Draw(img)
    draw.line([(0, 127), (0, 0), (127, 0)], fill=(255, 90, 90, 255), width=2)
    draw.line([(1, 127), (127, 127), (127, 1)], fill=(150, 10, 10, 255), width=2)
    draw.ellipse([20, 20, 45, 45], fill=(255, 180, 180, 255))
    add_noise(img, 2)
    img.save(path)

def generate_keycard(path, is_lvl2):
    if is_lvl2:
        img = Image.new("RGBA", (128, 128), (45, 45, 45, 255)) # Dark grey body
        stripe_color = (220, 180, 20, 255) # Gold stripe
    else:
        img = Image.new("RGBA", (128, 128), (235, 235, 235, 255)) # White body
        stripe_color = (25, 110, 200, 255) # Blue stripe
        
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 127, 127], outline=(100, 100, 100, 255), width=2)
    draw.rectangle([0, 48, 127, 80], fill=stripe_color)
    draw.rectangle([16, 16, 44, 40], fill=(210, 212, 215, 255) if is_lvl2 else (210, 170, 40, 255))
    draw.rectangle([16, 16, 44, 40], outline=(100, 100, 100, 255), width=1)
    draw.line([(30, 16), (30, 40)], fill=(120, 120, 120, 255), width=1)
    draw.line([(16, 28), (44, 28)], fill=(120, 120, 120, 255), width=1)
    draw.line([(60, 24), (110, 24)], fill=(80, 80, 80, 255), width=3)
    draw.line([(60, 32), (95, 32)], fill=(80, 80, 80, 255), width=3)
    draw.rectangle([80, 90, 112, 114], fill=(20, 20, 20, 255))
    draw.rectangle([80, 90, 112, 114], outline=(255, 255, 255, 255), width=1)
    
    if is_lvl2:
        draw.line([(88, 95), (104, 95)], fill=(220, 180, 20, 255), width=2)
        draw.line([(104, 95), (104, 103)], fill=(220, 180, 20, 255), width=2)
        draw.line([(104, 103), (88, 103)], fill=(220, 180, 20, 255), width=2)
        draw.line([(88, 103), (88, 109)], fill=(220, 180, 20, 255), width=2)
        draw.line([(88, 109), (104, 109)], fill=(220, 180, 20, 255), width=2)
    else:
        draw.line([(96, 95), (96, 109)], fill=(25, 110, 200, 255), width=2)
        draw.line([(92, 97), (96, 95)], fill=(25, 110, 200, 255), width=2)
        draw.line([(90, 109), (102, 109)], fill=(25, 110, 200, 255), width=2)
        
    add_noise(img, 2)
    img.save(path)

# --- Drawer textures ---
def generate_drawer_wood(path):
    """Warm polished oak drawer panel with plank lines"""
    img = Image.new("RGBA", (128, 128), (165, 115, 65, 255))
    draw = ImageDraw.Draw(img)
    # Horizontal wood grain lines
    for i in range(12):
        y = i * 11 + random.randint(-3, 3)
        pts = []
        for x in range(0, 140, 8):
            pts.append((x, y + int(4 * random.random())))
        draw.line(pts, fill=(130, 85, 45, 255), width=random.randint(1, 2))
    # Light highlight on top edge
    draw.line([(0, 0), (127, 0)], fill=(200, 155, 100, 255), width=2)
    draw.line([(0, 0), (0, 127)], fill=(190, 145, 90, 255), width=2)
    # Dark shadow on bottom edge
    draw.line([(0, 127), (127, 127)], fill=(100, 65, 35, 255), width=2)
    draw.line([(127, 0), (127, 127)], fill=(110, 70, 40, 255), width=2)
    add_noise(img, 3)
    img.save(path)

def generate_drawer_front(path):
    """Drawer front with inset panel and handle groove"""
    img = Image.new("RGBA", (128, 128), (155, 105, 60, 255))
    draw = ImageDraw.Draw(img)
    # Outer frame
    draw.rectangle([0, 0, 127, 127], outline=(120, 80, 40, 255), width=3)
    # Inner panel inset
    draw.rectangle([10, 10, 117, 117], outline=(180, 135, 80, 255), width=2)
    draw.rectangle([12, 12, 115, 115], fill=(165, 115, 65, 255))
    # Wood grain on panel
    for i in range(6):
        y = 20 + i * 16 + random.randint(-3, 3)
        pts = []
        for x in range(14, 114, 8):
            pts.append((x, y + int(3 * random.random())))
        draw.line(pts, fill=(135, 90, 50, 255), width=1)
    # Handle: small dark knob
    draw.ellipse([54, 58, 74, 70], fill=(80, 55, 30, 255))
    draw.ellipse([56, 60, 72, 68], fill=(100, 70, 40, 255))
    # Specular on knob
    draw.ellipse([60, 61, 66, 65], fill=(140, 105, 70, 255))
    add_noise(img, 2)
    img.save(path)

def generate_drawer_inside(path):
    """Dark interior of an open drawer"""
    img = Image.new("RGBA", (128, 128), (70, 55, 40, 255))
    draw = ImageDraw.Draw(img)
    # Bottom grain
    for i in range(10):
        y = i * 13 + random.randint(-2, 2)
        pts = []
        for x in range(0, 140, 10):
            pts.append((x, y + int(3 * random.random())))
        draw.line(pts, fill=(55, 42, 30, 255), width=1)
    # Subtle shadow around edges
    draw.rectangle([0, 0, 127, 127], outline=(45, 35, 25, 255), width=4)
    add_noise(img, 2)
    img.save(path)

def generate_drawer_handle(path):
    """Metallic drawer handle / knob"""
    img = Image.new("RGBA", (128, 128), (140, 135, 125, 255))
    draw = ImageDraw.Draw(img)
    # Brushed metal lines
    for _ in range(200):
        x = random.randint(0, 127)
        y = random.randint(0, 127)
        w = random.randint(10, 40)
        draw.line([(x, y), (x + w, y)], fill=(165, 160, 150, 255), width=1)
        draw.line([(x, y+1), (x+w, y+1)], fill=(120, 115, 105, 255), width=1)
    # Edge
    draw.line([(0, 0), (127, 0), (127, 127)], fill=(90, 85, 75, 255), width=2)
    draw.line([(0, 0), (0, 127), (127, 127)], fill=(180, 175, 165, 255), width=2)
    add_noise(img, 2)
    img.save(path)

# --- Child class item textures ---
def generate_leather(path):
    """Brown leather texture for baseball bat grip, backpack"""
    img = Image.new("RGBA", (128, 128), (130, 80, 40, 255))
    draw = ImageDraw.Draw(img)
    # Leather grain pattern
    for _ in range(80):
        x = random.randint(0, 120)
        y = random.randint(0, 120)
        w = random.randint(4, 15)
        h = random.randint(4, 15)
        draw.rectangle([x, y, x+w, y+h], outline=(105, 65, 30, 255), width=1)
    draw.line([(0, 0), (127, 0)], fill=(160, 110, 60, 255), width=2)
    draw.line([(0, 127), (127, 127)], fill=(90, 55, 25, 255), width=2)
    add_noise(img, 3)
    img.save(path)

def generate_rubber(path):
    """Dark rubber texture for grips"""
    img = Image.new("RGBA", (128, 128), (55, 55, 55, 255))
    draw = ImageDraw.Draw(img)
    # Diamond grip pattern
    for y in range(0, 128, 8):
        for x in range(0, 128, 8):
            offset = 4 if (y // 8) % 2 else 0
            draw.polygon([(x+offset+4, y), (x+offset+8, y+4), (x+offset+4, y+8), (x+offset, y+4)],
                        outline=(70, 70, 70, 255))
    add_noise(img, 2)
    img.save(path)

def generate_camera_body(path):
    """Dark camera body with subtle texture"""
    img = Image.new("RGBA", (128, 128), (35, 35, 38, 255))
    draw = ImageDraw.Draw(img)
    # Leatherette texture
    for _ in range(100):
        x = random.randint(0, 124)
        y = random.randint(0, 124)
        draw.rectangle([x, y, x+3, y+3], outline=(50, 50, 55, 255), width=1)
    draw.line([(0, 0), (127, 0)], fill=(60, 60, 65, 255), width=1)
    draw.line([(0, 127), (127, 127)], fill=(20, 20, 22, 255), width=1)
    add_noise(img, 1)
    img.save(path)

def generate_lens_glass(path):
    """Camera lens - dark with circular rings"""
    img = Image.new("RGBA", (128, 128), (25, 28, 35, 255))
    draw = ImageDraw.Draw(img)
    cx, cy = 64, 64
    for r in [55, 45, 35, 25, 15]:
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], outline=(50, 55, 65, 255), width=2)
    # Center glare
    draw.ellipse([cx-8, cy-12, cx+4, cy-4], fill=(120, 140, 180, 80))
    add_noise(img, 1)
    img.save(path)

def generate_fabric_green(path):
    """Military-green canvas fabric for backpack"""
    img = Image.new("RGBA", (128, 128), (85, 105, 65, 255))
    draw = ImageDraw.Draw(img)
    # Weave pattern
    for y in range(0, 128, 4):
        for x in range(0, 128, 4):
            c = random.choice([(80, 100, 60, 255), (90, 110, 70, 255)])
            draw.rectangle([x, y, x+3, y+3], fill=c)
    draw.line([(0, 0), (127, 0)], fill=(110, 130, 85, 255), width=2)
    draw.line([(0, 127), (127, 127)], fill=(60, 75, 45, 255), width=2)
    add_noise(img, 2)
    img.save(path)

def generate_iron_rust(path):
    """Rusty iron for bear trap"""
    img = Image.new("RGBA", (128, 128), (150, 130, 110, 255))
    draw = ImageDraw.Draw(img)
    # Rust spots
    for _ in range(40):
        x = random.randint(0, 120)
        y = random.randint(0, 120)
        r = random.randint(3, 10)
        draw.ellipse([x, y, x+r, y+r], fill=(165, 100, 55, 255))
    # Scratches
    for _ in range(60):
        x1 = random.randint(0, 127)
        y1 = random.randint(0, 127)
        x2 = x1 + random.randint(-15, 15)
        y2 = y1 + random.randint(-15, 15)
        draw.line([(x1, y1), (x2, y2)], fill=(175, 170, 160, 255), width=1)
    draw.line([(0, 0), (127, 0)], fill=(180, 165, 145, 255), width=2)
    draw.line([(0, 127), (127, 127)], fill=(110, 95, 80, 255), width=2)
    add_noise(img, 4)
    img.save(path)

def generate_skin_tone(path):
    """Skin-colored mask texture for neighbor mask"""
    img = Image.new("RGBA", (128, 128), (210, 170, 130, 255))
    draw = ImageDraw.Draw(img)
    # Subtle skin variation
    for _ in range(50):
        x = random.randint(0, 120)
        y = random.randint(0, 120)
        r = random.randint(5, 20)
        c = random.choice([(215, 175, 135, 255), (205, 165, 125, 255)])
        draw.ellipse([x, y, x+r, y+r], fill=c)
    add_noise(img, 2)
    img.save(path)

def generate_blue_glow(path):
    """Glowing blue screen for decoy console"""
    img = Image.new("RGBA", (128, 128), (20, 60, 160, 255))
    draw = ImageDraw.Draw(img)
    # Scanlines
    for y in range(0, 128, 3):
        draw.line([(0, y), (127, y)], fill=(30, 75, 180, 255), width=1)
    # Center glow
    cx, cy = 64, 64
    for r in range(50, 0, -5):
        alpha = min(255, 80 + (50 - r) * 3)
        c = (40 + (50-r)*2, 80 + (50-r)*2, 200, alpha)
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=c)
    draw.rectangle([0, 0, 127, 127], outline=(10, 30, 80, 255), width=3)
    add_noise(img, 1)
    img.save(path)

if __name__ == "__main__":
    base = "D:\\codingan\\b-secret\\secretneighbor_resources\\assets\\minecraft\\textures"
    block_dir = os.path.join(base, "block", "materials")
    item_dir = os.path.join(base, "item", "materials")

    os.makedirs(block_dir, exist_ok=True)
    os.makedirs(item_dir, exist_ok=True)

    # Generate block materials
    generate_color_panel(os.path.join(block_dir, "red.png"), 200, 25, 25, 240, 70, 70, 130, 10, 10)
    generate_color_panel(os.path.join(block_dir, "blue.png"), 25, 70, 200, 70, 110, 255, 10, 35, 130)
    generate_color_panel(os.path.join(block_dir, "green.png"), 25, 160, 45, 60, 210, 85, 10, 100, 25)
    generate_color_panel(os.path.join(block_dir, "yellow.png"), 220, 180, 20, 255, 220, 60, 150, 120, 10)
    generate_color_panel(os.path.join(block_dir, "purple.png"), 130, 25, 180, 180, 60, 240, 80, 10, 120)
    generate_color_panel(os.path.join(block_dir, "orange.png"), 220, 110, 15, 255, 150, 50, 150, 70, 10)

    generate_silver(os.path.join(block_dir, "silver.png"))
    generate_brown_wood(os.path.join(block_dir, "brown_wood.png"))
    generate_black(os.path.join(block_dir, "black.png"))
    generate_white(os.path.join(block_dir, "white.png"))
    generate_l_yellow(os.path.join(block_dir, "l_yellow.png"))

    # Generate item materials
    generate_dark_brown(os.path.join(item_dir, "dark_brown.png"))
    generate_screen(os.path.join(item_dir, "screen.png"))
    generate_color_panel(os.path.join(item_dir, "red.png"), 200, 25, 25, 240, 70, 70, 130, 10, 10)
    generate_light_red(os.path.join(item_dir, "light_red.png"))
    generate_black(os.path.join(item_dir, "black.png"))
    generate_silver(os.path.join(item_dir, "silver.png"))
    
    # Generate Keycard textures
    generate_keycard(os.path.join(item_dir, "keycard_lvl1.png"), False)
    generate_keycard(os.path.join(item_dir, "keycard_lvl2.png"), True)

    # --- DRAWER textures ---
    generate_drawer_wood(os.path.join(block_dir, "drawer_wood.png"))
    generate_drawer_front(os.path.join(block_dir, "drawer_front.png"))
    generate_drawer_inside(os.path.join(block_dir, "drawer_inside.png"))
    generate_drawer_handle(os.path.join(block_dir, "drawer_handle.png"))

    # --- CHILD CLASS + NEIGHBOR item textures ---
    generate_leather(os.path.join(block_dir, "leather.png"))
    generate_rubber(os.path.join(block_dir, "rubber.png"))
    generate_camera_body(os.path.join(block_dir, "camera_body.png"))
    generate_lens_glass(os.path.join(block_dir, "lens_glass.png"))
    generate_fabric_green(os.path.join(block_dir, "fabric_green.png"))
    generate_iron_rust(os.path.join(block_dir, "iron_rust.png"))
    generate_skin_tone(os.path.join(block_dir, "skin_tone.png"))
    generate_blue_glow(os.path.join(block_dir, "blue_glow.png"))

    print("Successfully generated all 128x128 HD textures!")

