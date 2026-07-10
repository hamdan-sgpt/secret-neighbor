import os
import json

base_dir = r"D:\codingan\b-secret"
dest_models_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "models", "item")
os.makedirs(dest_models_dir, exist_ok=True)

# 1. Closed Model (0 degrees)
closed_model = {
    "textures": {
        "body": "block/materials/fridge_body",
        "front": "block/materials/fridge_front",
        "handle": "block/materials/fridge_handle"
    },
    "elements": [
        {
            "__comment": "Body Frame",
            "from": [1, 0, 1.5],
            "to": [15, 32, 15.5],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Door Closed",
            "from": [1.5, 0.5, 0.5],
            "to": [14.5, 31.5, 1.5],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Handle Closed",
            "from": [12, 10, -0.5],
            "to": [13, 22, 0.5],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        }
    ],
    "display": {
        "head": {
            "rotation": [0, 0, 0],
            "translation": [0, -10.0, 0],
            "scale": [1.6, 1.6, 1.6]
        }
    }
}

# 2. Half Open Model (-45 degrees rotation around hinge at X=1.5, Z=1.5)
half_model = {
    "textures": {
        "body": "block/materials/fridge_body",
        "front": "block/materials/fridge_front",
        "handle": "block/materials/fridge_handle"
    },
    "elements": [
        {
            "__comment": "Body Frame",
            "from": [1, 0, 1.5],
            "to": [15, 32, 15.5],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Door Half Open",
            "from": [1.5, 0.5, 0.5],
            "to": [14.5, 31.5, 1.5],
            "rotation": {"origin": [1.5, 0.5, 1.5], "axis": "y", "angle": 45.0},
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Handle Half Open",
            "from": [12, 10, -0.5],
            "to": [13, 22, 0.5],
            "rotation": {"origin": [1.5, 10.0, 1.5], "axis": "y", "angle": 45.0},
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        }
    ],
    "display": {
        "head": {
            "rotation": [0, 0, 0],
            "translation": [0, -10.0, 0],
            "scale": [1.6, 1.6, 1.6]
        }
    }
}

# 3. Fully Open Model (90 degrees rotation)
# Door: parallel to the left side (west side).
# Closed door was X: [1.5, 14.5], Z: [0.5, 1.5]. Rotated 90 degrees around [1.5, 1.5]:
# X becomes [0.5, 1.5], Z becomes [1.5, 14.5].
# Handle: Closed handle was X: [12, 13], Z: [-0.5, 0.5]. Rotated 90 degrees around [1.5, 1.5]:
# X becomes [-0.5, 0.5], Z becomes [12, 13].
open_model = {
    "textures": {
        "body": "block/materials/fridge_body",
        "front": "block/materials/fridge_front",
        "handle": "block/materials/fridge_handle"
    },
    "elements": [
        {
            "__comment": "Body Frame",
            "from": [1, 0, 1.5],
            "to": [15, 32, 15.5],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Door Open (90 deg)",
            "from": [0.5, 0.5, 1.5],
            "to": [1.5, 31.5, 14.5],
            "faces": {
                "west":  {"uv": [0, 0, 16, 16], "texture": "#front"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#body"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#body"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#body"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#body"}
            }
        },
        {
            "__comment": "Handle Open (90 deg)",
            "from": [-0.5, 10, 12],
            "to": [0.5, 22, 13],
            "faces": {
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        }
    ],
    "display": {
        "head": {
            "rotation": [0, 0, 0],
            "translation": [0, -10.0, 0],
            "scale": [1.6, 1.6, 1.6]
        }
    }
}

# Write JSON files
with open(os.path.join(dest_models_dir, "fridge_closed.json"), "w") as f:
    json.dump(closed_model, f, indent=2)

with open(os.path.join(dest_models_dir, "fridge_half.json"), "w") as f:
    json.dump(half_model, f, indent=2)

with open(os.path.join(dest_models_dir, "fridge_open.json"), "w") as f:
    json.dump(open_model, f, indent=2)

print("Generated all refrigerator JSON models successfully!")
