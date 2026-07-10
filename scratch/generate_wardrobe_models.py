import os
import json

base_dir = r"D:\codingan\b-secret"
dest_models_dir = os.path.join(base_dir, "secretneighbor_resources", "assets", "minecraft", "models", "item")
os.makedirs(dest_models_dir, exist_ok=True)

# 1. Wardrobe Closed
closed_model = {
    "textures": {
        "wood": "block/materials/wardrobe_wood",
        "door": "block/materials/wardrobe_door",
        "handle": "block/materials/wardrobe_handle"
    },
    "elements": [
        {
            "__comment": "Body Cabinet Frame",
            "from": [0, 0, 1.5],
            "to": [16, 32, 16],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Door Closed",
            "from": [0.5, 0.5, 0.5],
            "to": [8.0, 31.5, 1.5],
            "faces": {
                "north": {"uv": [0, 0, 8, 16], "texture": "#door"},
                "south": {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "down":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "west":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "east":  {"uv": [0, 0, 8, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Right Door Closed",
            "from": [8.0, 0.5, 0.5],
            "to": [15.5, 31.5, 1.5],
            "faces": {
                "north": {"uv": [8, 0, 16, 16], "texture": "#door"},
                "south": {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "down":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "west":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "east":  {"uv": [8, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Handle Closed",
            "from": [6.5, 12, -0.2],
            "to": [7.2, 20, 0.5],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        },
        {
            "__comment": "Right Handle Closed",
            "from": [8.8, 12, -0.2],
            "to": [9.5, 20, 0.5],
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

# 2. Wardrobe Half Open (Doors rotated 45 degrees around hinges at left [0.5,0.5,1.5] and right [15.5,0.5,1.5])
half_model = {
    "textures": {
        "wood": "block/materials/wardrobe_wood",
        "door": "block/materials/wardrobe_door",
        "handle": "block/materials/wardrobe_handle"
    },
    "elements": [
        {
            "__comment": "Body Cabinet Frame",
            "from": [0, 0, 1.5],
            "to": [16, 32, 16],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Door Half Open",
            "from": [0.5, 0.5, 0.5],
            "to": [8.0, 31.5, 1.5],
            "rotation": {"origin": [0.5, 0.5, 1.5], "axis": "y", "angle": -45.0},
            "faces": {
                "north": {"uv": [0, 0, 8, 16], "texture": "#door"},
                "south": {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "down":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "west":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "east":  {"uv": [0, 0, 8, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Right Door Half Open",
            "from": [8.0, 0.5, 0.5],
            "to": [15.5, 31.5, 1.5],
            "rotation": {"origin": [15.5, 0.5, 1.5], "axis": "y", "angle": 45.0},
            "faces": {
                "north": {"uv": [8, 0, 16, 16], "texture": "#door"},
                "south": {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "down":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "west":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "east":  {"uv": [8, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Handle Half Open",
            "from": [6.5, 12, -0.2],
            "to": [7.2, 20, 0.5],
            "rotation": {"origin": [0.5, 12, 1.5], "axis": "y", "angle": -45.0},
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        },
        {
            "__comment": "Right Handle Half Open",
            "from": [8.8, 12, -0.2],
            "to": [9.5, 20, 0.5],
            "rotation": {"origin": [15.5, 12, 1.5], "axis": "y", "angle": 45.0},
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

# 3. Wardrobe Fully Open (Doors rotated 90 degrees outward)
# Left door: X from 0.5 to 1.5, Z from 1.5 to 9.0 (hinge at [0.5, 1.5])
# Right door: X from 14.5 to 15.5, Z from 1.5 to 9.0 (hinge at [15.5, 1.5])
open_model = {
    "textures": {
        "wood": "block/materials/wardrobe_wood",
        "door": "block/materials/wardrobe_door",
        "handle": "block/materials/wardrobe_handle"
    },
    "elements": [
        {
            "__comment": "Body Cabinet Frame",
            "from": [0, 0, 1.5],
            "to": [16, 32, 16],
            "faces": {
                "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Door Open (90 deg)",
            "from": [0.5, 0.5, 1.5],
            "to": [1.5, 31.5, 9.0],
            "faces": {
                "west":  {"uv": [0, 0, 8, 16], "texture": "#door"},
                "east":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "up":    {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "down":  {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "north": {"uv": [0, 0, 8, 16], "texture": "#wood"},
                "south": {"uv": [0, 0, 8, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Right Door Open (90 deg)",
            "from": [14.5, 0.5, 1.5],
            "to": [15.5, 31.5, 9.0],
            "faces": {
                "east":  {"uv": [8, 0, 16, 16], "texture": "#door"},
                "west":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "up":    {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "down":  {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "north": {"uv": [8, 0, 16, 16], "texture": "#wood"},
                "south": {"uv": [8, 0, 16, 16], "texture": "#wood"}
            }
        },
        {
            "__comment": "Left Handle Open",
            "from": [-0.2, 12, 7.5],
            "to": [0.5, 20, 8.2],
            "faces": {
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
            }
        },
        {
            "__comment": "Right Handle Open",
            "from": [15.5, 12, 7.5],
            "to": [16.2, 20, 8.2],
            "faces": {
                "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
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
with open(os.path.join(dest_models_dir, "wardrobe_closed.json"), "w") as f:
    json.dump(closed_model, f, indent=2)

with open(os.path.join(dest_models_dir, "wardrobe_half.json"), "w") as f:
    json.dump(half_model, f, indent=2)

with open(os.path.join(dest_models_dir, "wardrobe_open.json"), "w") as f:
    json.dump(open_model, f, indent=2)

print("Generated all wardrobe JSON models successfully!")
