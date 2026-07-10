import os
import json

base_dir = r"d:\codingan\b-secret\secretneighbor_resources\assets\minecraft\models\item"

def create_stage(stage_num, drawer_from_z, handle_from_z, handle_to_z):
    # Base layout from drawer_open.json
    model = {
        "textures": {
            "wood": "block/materials/drawer_wood",
            "front": "block/materials/drawer_front",
            "handle": "block/materials/drawer_handle",
            "inside": "block/materials/drawer_inside"
        },
        "elements": [
            {
                "__comment": "Body Frame",
                "from": [2, 0, 4],
                "to": [14, 12, 12],
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
                "__comment": "Top Drawer Pulled Out",
                "from": [3, 7, drawer_from_z],
                "to": [13, 11, 4],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#inside"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#wood"}
                }
            },
            {
                "__comment": "Top Drawer Handle",
                "from": [7, 8.5, handle_from_z],
                "to": [9, 9.5, handle_to_z],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
                }
            },
            {
                "__comment": "Bottom Drawer Front (still closed)",
                "from": [3, 1, 3.5],
                "to": [13, 6, 4],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#front"}
                }
            },
            {
                "__comment": "Bottom Drawer Handle",
                "from": [7, 3, 3],
                "to": [9, 4, 3.5],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#handle"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#handle"}
                }
            }
        ],
        "display": {
            "head": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [2.1333, 2.1333, 2.1333]
            }
        }
    }
    
    file_path = os.path.join(base_dir, f"drawer_stage{stage_num}.json")
    with open(file_path, 'w') as f:
        json.dump(model, f, indent=2)
    print(f"Generated {file_path}")

if __name__ == "__main__":
    # Stage 1: Z translated from 3.5 to 2.625 (25% open)
    create_stage(1, 2.625, 2.125, 2.625)
    # Stage 2: Z translated from 3.5 to 1.75 (50% open)
    create_stage(2, 1.75, 1.25, 1.75)
    # Stage 3: Z translated from 3.5 to 0.875 (75% open)
    create_stage(3, 0.875, 0.375, 0.875)
