import os
import requests
import subprocess


def install():
    print("Downloading ...")
    releases_url = "https://api.github.com/repos/tvhee-dev/DrillsterBot/releases"
    version = requests.get(releases_url).json()[0]
    assets = version["assets"]

    for asset in assets:
        if asset["name"].endswith(".jar"):
            download_url = asset["browser_download_url"]
            desktop = os.path.join(os.path.join(os.environ["USERPROFILE"]), "Desktop")
            filename = os.path.join(desktop, "DrillsterBot.jar")
            download_link_response = requests.get(download_url)

            with open(filename, "wb") as file:
                file.write(download_link_response.content)

            try:
                subprocess.run(["java", "-version"], capture_output=True, check=True)
            except subprocess.CalledProcessError:
                print("Downloading Java ...")
                r = requests.get("https://javadl.oracle.com/webapps/download/AutoDL?BundleId"
                                 "=248242_ce59cff5c23f4e2eaf4e778a117d4c5b", allow_redirects=True)
                open('javainstall.exe', 'wb').write(r.content)
                os.system(os.getcwd() + "\javainstall.exe /s")
                os.remove("javainstall.exe")

            print(f"Successfully downloaded version {version['tag_name']}!")

            try:
                os.remove(f"{desktop}/DrillsterBot-v3.0.0.exe")
            except:
                pass

            os.system(f'move DrillsterBotInstaller.exe ' + r'"%localappdata%\temp"')
            os.system(f"java -jar \"{desktop}/DrillsterBot.jar\"")
            return

    print("No jarfile found!")


install()
