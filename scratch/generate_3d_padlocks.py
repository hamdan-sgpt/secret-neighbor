import os
import json

base_dir = r"d:\codingan\b-secret\secretneighbor_resources\assets\minecraft\models\item"

colors = {
    "red": "minecraft:block/red_concrete",
    "blue": "minecraft:block/blue_concrete",
    "green": "minecraft:block/lime_concrete",
    "yellow": "minecraft:block/yellow_concrete",
    "purple": "minecraft:block/purple_concrete",
    "orange": "minecraft:block/orange_concrete"
}

def generate_3d_padlock(color_name, body_texture):
    model = {
        "textures": {
            "body": body_texture,
            "metal": "minecraft:block/iron_block",
            "keyhole": "minecraft:block/black_concrete"
        },
        "elements": [
            {
                "__comment": "Lock Body",
                "from": [5, 2, 6.5],
                "to": [11, 8, 9.5],
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
                "__comment": "Left Shackle Leg",
                "from": [5.5, 8, 7.5],
                "to": [6.5, 12, 8.5],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#metal"}
                }
            },
            {
                "__comment": "Right Shackle Leg",
                "from": [9.5, 8, 7.5],
                "to": [10.5, 12, 8.5],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#metal"}
                }
            },
            {
                "__comment": "Top Shackle Arch",
                "from": [5.5, 12, 7.5],
                "to": [10.5, 13.5, 8.5],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "north": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "west":  {"uv": [0, 0, 16, 16], "texture": "#metal"},
                    "east":  {"uv": [0, 0, 16, 16], "texture": "#metal"}
                }
            },
            {
                "__comment": "Keyhole",
                "from": [7.5, 3.5, 6.4],
                "to": [8.5, 5.5, 6.5],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#keyhole"}
                }
            }
        ],
        "display": {
            "fixed": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [1.5, 1.5, 1.5]
            },
            "head": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [1.5, 1.5, 1.5]
            }
        }
    }
    
    file_path = os.path.join(base_dir, f"padlock_{color_name}.json")
    with open(file_path, 'w') as f:
        json.dump(model, f, indent=2)
    print(f"Generated 3D padlock model: {file_path}")

if __name__ == "__main__":
    os.makedirs(base_dir, exist_ok=True)
    for color, texture in colors.items():
        generate_3d_padlock(color, texture)
