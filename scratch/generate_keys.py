import os

base_dir = "d:/codingan/b-secret/secretneighbor_resources/assets/minecraft/models/item"
key_path = os.path.join(base_dir, "key.json")

with open(key_path, 'r') as f:
    content = f.read()

colors = ["red", "blue", "green", "yellow", "purple", "orange"]

for color in colors:
    new_content = content.replace('"particle": "block/materials/red"', f'"particle": "block/materials/{color}"')
    new_content = new_content.replace('"texture": "block/materials/red"', f'"texture": "block/materials/{color}"')
    new_content = new_content.replace('"silver": "block/materials/red"', '"silver": "block/materials/silver"')
    
    out_path = os.path.join(base_dir, f"key_{color}.json")
    with open(out_path, 'w') as f_out:
        f_out.write(new_content)

print("Generated 6 colored key models successfully!")
