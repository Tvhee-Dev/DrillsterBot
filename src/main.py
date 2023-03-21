import threading
import inquirer
import time
import os
import json
import drillster
import browser_cookie3


def start():
    set_token = False

    for cookie in browser_cookie3.load(domain_name="www.drillster.com"):
        if cookie.name == "stroop":
            drillster.set_token(cookie.value)
            set_token = True

    if set_token is False:
        print("Please login to Drillster on your favourite webbrowser!")
        exit()

    start_drills(select_drills())


def select_drills():
    repertoire = drillster.get_repertoire()

    playable = inquirer.prompt(
        [inquirer.List('Playable', message='Select playable',
                       choices=[(index["name"], index["id"]) for index in repertoire])])["Playable"]

    if [item["type"] for item in repertoire if item["id"] == playable][0] == "COURSE":
        print("Loading course...")
        results = drillster.get_course_content(playable)

        return inquirer.prompt(
            [inquirer.Checkbox('Drills', message='Select drills', choices=extract_playable_drills(results))])["Drills"]

    elif [item["type"] for item in repertoire if item["id"] == playable][0] == "DRILL":
        return playable
    else:
        print("Cannot play tests using DrillsterBot")
        exit()


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
    print(f"Starting {drill_id}")
    start_time = time.time()
    current_drill = drillster.Drill(drill_id)

    # Load existing dictionary of questions and answers from a file if it exists, otherwise create an empty dictionary
    if os.path.exists(f"./wordlists/{drill_id}.json"):
        with open(f"./wordlists/{drill_id}.json", "r") as file_content:
            stored_wordlist = json.load(file_content)
    else:
        stored_wordlist = {}

    proficiency = 0

    while proficiency < 100:
        # Get a question from the drill
        question_object = current_drill.get_question()

        # Check if the question is already in the dictionary of questions and answers
        if question_object["ask"]["term"]["value"] not in stored_wordlist:
            # Answer the question and add the question-answer pair to the dictionary
            answer_object = current_drill.answer_question(answer="")
            stored_wordlist[question_object["ask"]["term"]["value"]] = \
                answer_object["evaluation"]["termEvaluations"][1]["value"]
        else:
            # Answer the question using the previously recorded answer from the dictionary
            answer_object = current_drill.answer_question(
                answer=stored_wordlist[question_object["ask"]["term"]["value"]])

        # Check the overall proficiency level and continue answering questions until it is greater than or equal to 100
        proficiency = answer_object["proficiency"]["overall"]

    # Save the updated dictionary of questions and answers to a file
    if os.path.exists("./wordlists/"):
        with open(f"./wordlists/{drill_id}.json", "w") as file_content:
            json.dump(stored_wordlist, file_content)
    else:
        os.mkdir("./wordlists/")
        with open(f"./wordlists/{drill_id}.json", "w") as file_content:
            json.dump(stored_wordlist, file_content)

    # Print a message indicating that the drill is completed and how long it took to complete
    print(f"Completed {drill_id} in {round(time.time() - start_time)} seconds")


def start_drills(drill_ids):
    # Create threads for each drill and start them
    threads = []

    for drill_id in drill_ids:
        thread = threading.Thread(target=start_drill, args=(drill_id,))
        threads.append(thread)
        thread.start()

    # Wait for all threads to finish before continuing
    for thread in threads:
        thread.join()


start()
