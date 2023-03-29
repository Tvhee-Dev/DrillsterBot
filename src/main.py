# Front end loader - user has to select the Drills / Courses here
import os
import time

import inquirer
import requests
import drillster
import browser_cookie3
import sys
import platform

current_version = "v2.0.2"


def start():
    if auto_update():
        return

    print("Welcome to DrillsterBot!")
    print("")
    print("To navigate, use the arrow keys and the ENTER key")
    print("^ = go up one element")
    print("v = go down one element")
    print("ENTER = continue")
    print("")
    print("Loading your repertoire...")
    print("")

    set_token = False

    for cookie in browser_cookie3.load(domain_name="www.drillster.com"):
        if cookie.name == "stroop":
            drillster.set_token(cookie.value)
            set_token = True

    if set_token is False:
        pause("No token found! Please login to Drillster on a browser and keep the tab open! Press any key to exit...")

    selected_drills = select_drills()

    if isinstance(selected_drills, list) and len(selected_drills) == 0:
        pause("You did not select any Drills! Press any key to exit...")

    start_time = time.time()
    drillster.start_drills(selected_drills)
    delta_time = round(time.time() - start_time)

    print("")
    print(f"DrillsterBot has finished the selected Drills in {delta_time} seconds!")


def auto_update():
    releases_url = "https://api.github.com/repos/tvhee-dev/DrillsterBot/releases"
    response = requests.get(releases_url)
    latest_release = response.json()[0]["tag_name"]

    if latest_release != current_version:
        print("Updating DrillsterBot...")
        download_url = response.json()[0]["assets"][0]["browser_download_url"]
        filename = os.path.join(os.getcwd(), f"DrillsterBot-{latest_release}.exe")
        download_link_response = requests.get(download_url)

        with open(filename, "wb") as file:
            file.write(download_link_response.content)

        print("Finished downloading update: ", filename)
        os.system(f'move DrillsterBot-{current_version}.exe ' + r'"%localappdata%\temp"')
        os.system(f"DrillsterBot-{latest_release}.exe")
        return True

    return False


def select_drills():
    repertoire = drillster.get_repertoire()
    playables = [(index["name"], index["id"]) for index in repertoire]
    playables.append(("Exit", "-1"))
    play_id = inquirer.prompt([inquirer.List("Playable", message="Selected playable", choices=playables)])["Playable"]

    if play_id == "-1":
        sys.exit()

    if [item["type"] for item in repertoire if item["id"] == play_id][0] == "COURSE":
        print("Loading course...")
        print("")

        course_drills = drillster.get_course_content(play_id)
        print("Use SPACE to select/deselect drills, then press ENTER to start")
        print("")

        drill_list = extract_playable_drills(course_drills)
        selected_drills = inquirer.prompt(
            [inquirer.Checkbox("Drills", message="Selected Drills",
                               choices=[("Play all drills", "PlayAll")] + drill_list)])["Drills"]

        if "PlayAll" in selected_drills:
            print("Playing all Drills in this course!")
            # Gets all ids from tuple list
            return [drill[1] for drill in drill_list]

        else:
            return selected_drills

    elif [item["type"] for item in repertoire if item["id"] == play_id][0] == "DRILL":
        return [play_id]

    else:
        pause("Cannot play tests using DrillsterBot")


# Define a function to extract drills from repertoire
def extract_playable_drills(repertoire_list):
    result = []

    for item in repertoire_list:
        if isinstance(item, dict):
            if "playable" in item:
                playable_drill = item["playable"]

                if "name" in playable_drill and "id" in playable_drill:
                    result.append((playable_drill["name"], playable_drill["id"]))

            else:
                result.extend(extract_playable_drills(item.values()))

        elif isinstance(item, list):
            result.extend(extract_playable_drills(item))

    return result


def pause(message="Press any key to exit..."):
    if platform.system() == "Windows":
        os.system(f"echo {message}")
        os.system("pause >nul")
    else:
        os.system(f"/bin/bash -c 'read -s -n 1 -p \"{message}\"'")
    sys.exit()


start()
pause()
