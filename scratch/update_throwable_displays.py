import os
import json

base_dir = r"D:\codingan\b-secret"
models_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "models", "item")

# All throwable items that need proper display settings for ArmorStand head rendering
throwable_models = [
    "box.json", "chair.json", "tv.json", "sofa.json",
    "painting.json", "book_model.json", "tomato.json", "broom.json",
    "basketball.json", "pillow.json", "hat.json"
]

# Display settings optimized for head-slot rendering on armor stands
# Scale 1.0 = fills 1 block. We want these objects to look ~0.6-0.8 blocks large when thrown
display_head = {
    "rotation": [0, 0, 0],
    "translation": [0, 0, 0],
    "scale": [1.0, 1.0, 1.0]
}

display_gui = {
    "rotation": [30, 225, 0],
    "translation": [0, 0, 0],
    "scale": [0.625, 0.625, 0.625]
}

display_ground = {
    "rotation": [0, 0, 0],
    "translation": [0, 3, 0],
    "scale": [0.25, 0.25, 0.25]
}

display_fixed = {
    "rotation": [0, 0, 0],
    "translation": [0, 0, 0],
    "scale": [0.5, 0.5, 0.5]
}

display_thirdperson_righthand = {
    "rotation": [75, 45, 0],
    "translation": [0, 2.5, 0],
    "scale": [0.375, 0.375, 0.375]
}

display_firstperson_righthand = {
    "rotation": [0, 45, 0],
    "translation": [0, 0, 0],
    "scale": [0.4, 0.4, 0.4]
}

display_settings = {
    "head": display_head,
    "gui": display_gui,
    "ground": display_ground,
    "fixed": display_fixed,
    "thirdperson_righthand": display_thirdperson_righthand,
    "firstperson_righthand": display_firstperson_righthand
}

updated_count = 0
for model_file in throwable_models:
    path = os.path.join(models_dir, model_file)
    if not os.path.exists(path):
        print(f"SKIP: {model_file} not found")
        continue
    
    with open(path, 'r') as f:
        model = json.load(f)
    
    model["display"] = display_settings
    
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    
    updated_count += 1
    print(f"Updated display settings: {model_file}")

print(f"\nDone! Updated {updated_count} throwable models with display settings.")
