import urllib.request
import json
import os

def list_and_download(repo, dest_folder):
    url = f"https://api.github.com/repos/{repo}/releases"
    req = urllib.request.Request(
        url, 
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    try:
        with urllib.request.urlopen(req) as response:
            releases = json.loads(response.read().decode())
            print(f"Found {len(releases)} releases.")
            
            # Look at the first (latest) release
            latest = releases[0]
            print(f"Latest release: {latest['tag_name']}")
            print("Assets:")
            for asset in latest.get('assets', []):
                print(f"  - {asset['name']} ({asset['browser_download_url']})")
                
            # Let's find the one containing '1.20' or '1.21' or 'FOR-MC1.20'
            target_asset = None
            for asset in latest.get('assets', []):
                name = asset['name'].lower()
                if name.endswith('.jar'):
                    if '1.20' in name or '20' in name or '1.21' in name:
                        target_asset = asset
                        break
            
            if target_asset is None:
                # If no specific 1.20/1.21 jar is found, let's fall back to any jar that is not 1.19 if available
                # or just print them.
                print("No specific 1.20+ jar found in latest release, checking other assets...")
                for asset in latest.get('assets', []):
                    if asset['name'].endswith('.jar'):
                        target_asset = asset
                        break
            
            if target_asset:
                name = target_asset['name']
                download_url = target_asset['browser_download_url']
                dest_path = os.path.join(dest_folder, name)
                print(f"Downloading target asset: {name}...")
                urllib.request.urlretrieve(download_url, dest_path)
                print("Download successful!")
                
                # Delete the old 1.19 jar if it exists in dest_folder
                old_jar = os.path.join(dest_folder, "DynamicLights-1.3.0-FOR-MC1.19.jar")
                if os.path.exists(old_jar) and name != "DynamicLights-1.3.0-FOR-MC1.19.jar":
                    os.remove(old_jar)
                    print(f"Removed incompatible old 1.19 jar: {old_jar}")
            else:
                print("No suitable jar file found.")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    dest = r"D:\codingan\exit_8\server\plugins"
    os.makedirs(dest, exist_ok=True)
    list_and_download("xCykrix/DynamicLights", dest)
