import os
import requests
import subprocess


def install():
    releases_url = "https://api.github.com/repos/tvhee-dev/DrillsterBot/releases"
    version = requests.get(releases_url).json()[0]
    version_id = version["node_id"]
    version_name = version["tag_name"]
    publish_date = version["published_at"].replace("T", " (").replace("Z", ")")
    author = version["author"]["login"]

    print(f"============================= DrillsterBotInstaller =============================")
    print(f"Version Name: {version_name}")
    print(f"Version ID: {version_id}")
    print(f"Published: {publish_date}")
    print(f"With <3 developed by: {author} and Artin_")
    print(f"=================================================================================")
    print(" ")
    print(f"Loading download information, please wait ...")
    print(" ")

    assets = version["assets"]

    for asset in assets:
        if asset["name"].endswith(".jar"):
            download_url = asset["browser_download_url"]
            filename = os.path.join(os.getcwd(), "DrillsterBot.jar")

            print(f"Found file {filename}, downloading...")
            print(" ")

            download_link_response = requests.get(download_url)

            with open(filename, "wb") as file:
                file.write(download_link_response.content)

            try:
                subprocess.run(["java", "-version"], capture_output=True, check=True)
                print("Java installation found, skipping installation of Java")
            except:
                print("Java installation was not found -> Downloading Java 1.8, please wait ...")
                r = requests.get("https://javadl.oracle.com/webapps/download/AutoDL?BundleId"
                                 "=248242_ce59cff5c23f4e2eaf4e778a117d4c5b", allow_redirects=True)
                open('javainstall.exe', 'wb').write(r.content)
                print("Installing Java 1.8, please wait ...")
                os.system("\"" + os.getcwd() + "\javainstall.exe \" /s")
                os.remove("javainstall.exe")

            print(" ")
            print(f"Successfully downloaded DrillsterBot version {version_name}!")
            print(" ")

            try:
                os.remove(f"{os.getcwd()}/DrillsterBot-v3.0.0.exe")
                os.system(f'move DrillsterBotInstaller.exe ' + r'"%localappdata%\temp"')
            except:
                pass

            print(f"============================= DrillsterBot V3 ===================================")
            print("Version 3 works with DrillsterBot.jar. Please make sure this file exists and you can open it")
            print("For quick access make sure to move \"DrillsterBot.jar\" to your Desktop folder")
            print("Enjoy!")
            print(f"=================================================================================")
            print(" ")
            input("Installation finished. Press any key to exit ...")
            return

    print(f"Could not gather the download information on the server for {version_name}!")
    print("Installation cancelled, please contact a developer!")
    print(" ")
    input("Installation failed. Press any key to exit ...")
    return


install()