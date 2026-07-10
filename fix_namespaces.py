import os
import json

model_dir = r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\models\item"

for root, _, files in os.walk(model_dir):
    for file in files:
        if file.endswith(".json"):
            path = os.path.join(root, file)
            try:
                with open(path, "r") as f:
                    data = json.load(f)
                
                modified = False
                if "textures" in data:
                    for key, val in data["textures"].items():
                        if isinstance(val, str) and val.startswith("item/") and not val.startswith("minecraft:"):
                            data["textures"][key] = "minecraft:" + val
                            modified = True
                
                # Also fix overrides or parents if they refer to item/
                if "overrides" in data:
                    for override in data["overrides"]:
                        if "model" in override and isinstance(override["model"], str) and override["model"].startswith("item/") and not override["model"].startswith("minecraft:"):
                            override["model"] = "minecraft:" + override["model"]
                            modified = True
                
                if modified:
                    with open(path, "w") as f:
                        json.dump(data, f, indent=2)
                    print(f"Fixed namespaces in {file}")
            except Exception as e:
                print(f"Error processing {file}: {e}")
