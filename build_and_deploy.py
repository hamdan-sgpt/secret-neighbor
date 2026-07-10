import os
import shutil
import subprocess

# 1. Run zip_pack.py
print("Running zip_pack.py...")
import zip_pack

# 2. Build with Maven
print("Running Maven build...")
maven_path = r"C:\Users\Pongo\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"
jdk_path = r"C:\Users\Pongo\.antigravity-ide\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64"

env = os.environ.copy()
env["JAVA_HOME"] = jdk_path
env["MAVEN_OPTS"] = "-Xmx192m -Xms64m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32M -XX:MaxMetaspaceSize=96M"

result = subprocess.run([maven_path, "clean", "package", "-Dmaven.compiler.fork=false"], env=env, capture_output=True, text=True)
print(result.stdout)
if result.returncode != 0:
    print("Maven build failed!")
    print(result.stderr)
    exit(1)

# 3. Copy target jar to server plugins
src_jar = os.path.join(os.path.dirname(os.path.abspath(__file__)), "target", "SecretNeighbor-1.0.0.jar")
dest_jar = r"D:\codingan\exit_8\server\plugins\SecretNeighbor-1.0.0.jar"

print(f"Copying {src_jar} to {dest_jar}...")
try:
    os.makedirs(os.path.dirname(dest_jar), exist_ok=True)
    shutil.copy2(src_jar, dest_jar)
    print("Deploy successful!")
except Exception as e:
    print(f"Could not deploy jar to server plugins: {e}")
    print(f"Build was successful. Jar is located at: {src_jar}")
