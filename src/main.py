import sys
import threading
import inquirer
import os
import json
import drillster
import browser_cookie3

current_drills = []


def start():
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

    start_drills(select_drills())


# Show menu to select courses/drills
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


# Define a function to play a drill given its ID
def start_drill(drill_id):
    current_drill = drillster.Drill(drill_id)
    #proficiency = 0
    #start_time = time.time()
    lock.acquire() # prevents multiple threads outputting to the same line
    print(f"Starting {current_drill.get_name()}...")
    lock.release()
    drill = drillster.Drill(drill_id)
    current_drills.append(drill)

    # Load existing dictionary of questions and answers from a file if it exists, otherwise create an empty dictionary
    if os.path.exists(f"./wordlists/{drill_id}.json"):
        with open(f"./wordlists/{drill_id}.json", "r") as file_content:
            stored_wordlist = json.load(file_content)
    else:
        stored_wordlist = {}

    while drill.continue_answering():
        # Get a question from the drill
        question_object = drill.get_question()
        question = question_object["ask"]["term"]["value"]
        answer_object: json

        # Check if the question is already in the dictionary of questions and answers
        if question not in stored_wordlist:
            # Answer the question and add the question-answer pair to the dictionary
            answer_object = current_drill.answer_question(answer="")
            if question_object["tell"]["composition"] != "SET":
                stored_wordlist[question_object["ask"]["term"]["value"]] = \
                    answer_object["evaluation"]["termEvaluations"][1]["value"]
            elif question_object["tell"]["composition"] == "SET":
                correct_answers=[]
                for ans in answer_object["evaluation"]["termEvaluations"]:
                    if ans["value"] != '':
                        correct_answers.append(ans["value"])
                stored_wordlist[question_object["ask"]["term"]["value"]] = \
                    correct_answers
            answer_object = drill.answer_question(answer="")
        else:
            # Answer the question using the previously recorded answer from the dictionary
            answer_object = drill.answer_question(answer=stored_wordlist[question])

        update_progressbar()

    # Save the updated dictionary of questions and answers to a file
    if not os.path.exists("./wordlists/"):
        os.mkdir("./wordlists/")

    with open(f"./wordlists/{drill_id}.json", "w") as file_content:
        json.dump(stored_wordlist, file_content)

    #lock.acquire()
    # Print a message indicating that the drill is completed and how long it took to complete
    #print(f"Completed {drill.get_name()} in {round(time.time() - start_time,1)} seconds")
    #lock.release()


def start_drills(drill_ids):
    # Create threads for each drill and start them
    threads = []
    global lock 
    lock = threading.Lock()
    for drill_id in drill_ids:
        thread = threading.Thread(target=start_drill, args=(drill_id,))
        threads.append(thread)
        thread.start()

    print(f"Progress: [                    ] 0% (0 / {len(drill_ids)} Drills completed)")

    # Wait for all threads to finish before continuing
    for thread in threads:
        thread.join()

    global current_drills
    current_drills = []

    print("")
    print("DrillsterBot has finished!")


def update_progressbar():
    global current_drills
    drill_amount = len(current_drills)
    percentage = 0
    completed = 0

    for drill in current_drills:
        # part / whole (for the correct calculation the start percentage has been removed
        # and this percentage is added to the total percentage with 1 / drill_amount
        percentage += (((drill.percentage - drill.start_percentage) / (100 - drill.start_percentage)) / drill_amount)

        if drill.percentage == 100:
            completed += 1

    line = "Progress: ["

    for index in range(21):
        if (index * 5) > percentage:
            line.join("=")
        else:
            line.join(" ")

    line.join(f"] {round(percentage)}% ({completed} / {drill_amount} Drills completed)")

    sys.stdout.write("\r")
    sys.stdout.write(line)


start()
