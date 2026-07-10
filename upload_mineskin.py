import urllib.request
import json
import base64
from PIL import Image
import io
import time

def process():
    print("Downscaling image...")
    # Open HD image
    with Image.open(r"D:\codingan\b-secret\hello-neighbor-hd-skin-repaired.png") as img:
        # Resize to 64x64 with Lanczos
        resized = img.resize((64, 64), Image.Resampling.LANCZOS)
        
        # Save to memory buffer
        buf = io.BytesIO()
        resized.save(buf, format='PNG')
        img_bytes = buf.getvalue()

    print("Uploading to Mineskin...")
    # Upload to Mineskin
    try:
        url = "https://api.mineskin.org/generate/upload"
        
        import requests
        # Using requests to upload multipart/form-data
        files = {'file': ('skin.png', img_bytes, 'image/png')}
        data = {'visibility': '0'} # Public
        
        response = requests.post(url, files=files, data=data)
        
        if response.status_code == 200:
            res = response.json()
            val = res['data']['texture']['value']
            sig = res['data']['texture']['signature']
            print("\nSUCCESS!")
            print("VALUE:", val)
            print("SIGNATURE:", sig)
        else:
            print("Failed. Status code:", response.status_code)
            print(response.text)
            
    except ImportError:
        print("requests module not installed, trying native urllib...")
        # Native multipart form upload is complex, so we'll just save the file and let the user do it if needed, or we install requests.
        pass

if __name__ == "__main__":
    process()
