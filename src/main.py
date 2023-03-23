# Front end loader - user has to select the Drills / Courses here
import os
import time
import inquirer
import requests
import drillster
import browser_cookie3

current_version = "2.0.0"


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
        print("No token found! Please login to Drillster on any webbrowser and keep the tab open!")
        exit()

    start_time = time.time()
    drillster.start_drills(select_drills())
    delta_time = round(time.time() - start_time)

    print("")
    print(f"DrillsterBot has finished the selected Drills in {delta_time} seconds!")


def auto_update():
    releases_url = "https://api.github.com/repos/tvhee-dev/DrillsterBot/releases"
    response = requests.get(releases_url)
    latest_release = response.json()[0]["tag_name"]

    if latest_release != current_version:
        download_url = response.json()[0]["assets"][0]["browser_download_url"]
        filename = os.path.join(os.getcwd(), f"main.exe")
        download_link_response = requests.get(download_url)

        with open(filename, "wb") as file:
            file.write(download_link_response.content)

        print("Finished downloading update: ", filename)
        os.system(f'move DrillsterBot-v{current_version}.exe ' + r'%localappdata%\temp')
        os.system(f'DrillsterBot-v{latest_release}')
        return True

    return False


def select_drills():
    repertoire = drillster.get_repertoire()
    playables = [(index["name"], index["id"]) for index in repertoire]
    playables.append(("Exit", "-1"))
    play_id = inquirer.prompt([inquirer.List("Playable", message="Selected playable", choices=playables)])["Playable"]

    if play_id == "-1":
        exit()

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
        print("Cannot play tests using DrillsterBot")
        exit()


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


start()
