import json
import os

def px(val):
    return val * (16.0 / 64.0)

def make_face(u1, v1, u2, v2):
    return {"uv": [px(u1), px(v1), px(u2), px(v2)], "texture": "#skin"}

def make_cube(origin, size, uv_offsets):
    # uv_offsets: [x, y, w, h, d]
    x, y, w, h, d = uv_offsets
    
    faces = {
        "up":    make_face(x+d, y, x+d+w, y+d),
        "down":  make_face(x+d+w, y, x+d+w+w, y+d),
        "east":  make_face(x, y+d, x+d, y+d+h),
        "north": make_face(x+d, y+d, x+d+w, y+d+h),
        "west":  make_face(x+d+w, y+d, x+d+w+d, y+d+h),
        "south": make_face(x+d+w+d, y+d, x+d+w+d+w, y+d+h)
    }
    
    to = [origin[0] + size[0], origin[1] + size[1], origin[2] + size[2]]
    
    return {
        "from": origin,
        "to": to,
        "faces": faces
    }

def generate_model():
    # Model origin (8,0,8) is center bottom of player
    head = make_cube([4, 24, 4], [8, 8, 8], [0, 0, 8, 8, 8])
    body = make_cube([4, 12, 6], [8, 12, 4], [16, 16, 8, 12, 4])
    right_arm = make_cube([12, 12, 6], [4, 12, 4], [40, 16, 4, 12, 4])
    left_arm = make_cube([0, 12, 6], [4, 12, 4], [32, 48, 4, 12, 4])
    right_leg = make_cube([8, 0, 6], [4, 12, 4], [0, 16, 4, 12, 4])
    left_leg = make_cube([4, 0, 6], [4, 12, 4], [16, 48, 4, 12, 4])
    
    model = {
        "credit": "Generated for HD Neighbor",
        "texture_size": [16, 16],
        "textures": {
            "skin": "item/neighbor_hd"
        },
        "elements": [head, body, right_arm, left_arm, right_leg, left_leg],
        "display": {
            "head": {
                "translation": [0, -24, 0],
                "scale": [1.6, 1.6, 1.6]
            }
        }
    }
    
    model_path = r"D:\codingan\b-secret\secretneighbor_resources\assets\minecraft\models\item\neighbor_body.json"
    os.makedirs(os.path.dirname(model_path), exist_ok=True)
    with open(model_path, "w") as f:
        json.dump(model, f, indent=4)
        
    print(f"Generated {model_path}")

if __name__ == "__main__":
    generate_model()
