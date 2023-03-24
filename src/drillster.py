# Drill thread manager - manages all the current Drillster threads and updates the progress bar
import math
import os
import threading
import time
import requests
import json

from requests import ConnectTimeout

header = {}
current_drills = []
thread_lock: threading.Lock
wordlists = {}


def set_token(drillster_token):
    global header
    header = {"Authorization": f"Bearer {drillster_token}"}


def get_repertoire():
    return requests.get("https://www.drillster.com/api/3/repertoire", headers=header).json()["playableRenditions"]


def get_course_content(playable):
    link = f"https://www.drillster.com/api/3/results?playable={playable}"

    try:
        return requests.get(link, headers=header).json()["results"]
    except EOFError:
        return requests.get(link, headers=header).json()["results"]


def start_drills(drill_ids):
    # Load the whole wordlist file
    global wordlists
    global current_drills
    global thread_lock

    if os.path.exists("wordlists.json"):
        with open("wordlists.json", "r") as file_content:

            file_read = file_content.read()

            if file_read != "":
                wordlists = json.loads(file_read)

    # Create threads for each drill and start them
    start_time = time.time()
    thread_lock = threading.Lock()

    for drill_id in drill_ids:
        drill = Drill(drill_id)
        current_drills.append(drill)

    # Update the percentage bar every second
    percentage = 0

    while percentage < 100:
        percentage = update_progressbar(start_time)
        time.sleep(1)

    # Save all the wordlists
    with open("wordlists.json", "w") as file_content:
        file_content.write(json.dumps(wordlists))

    # Reset the current_drills list
    current_drills = []


def update_progressbar(start_time):
    drill_amount = len(current_drills)
    percentage = 0
    completed = 0

    for drill in current_drills:
        # part / whole (for the correct calculation the start percentage has been removed
        # and this percentage is added to the total percentage with 1 / drill_amount

        # If the Drill is finished then add it as whole
        if drill.percentage == 100 or drill.start_percentage == 100:
            completed += 1
            percentage += 100
        else:
            # delta_percentage / total_percentage added for 1 / drill_amount drills
            delta_percentage = drill.percentage - drill.start_percentage
            total_percentage = 100 - drill.start_percentage
            drill_percentage = (delta_percentage / total_percentage) * 100
            percentage += drill_percentage

    # By dividing at the end of the calculation, the calculation is more accurate
    percentage = percentage / drill_amount
    progressbar_percent = max([percentage, 0])
    characters_filled = round(progressbar_percent / 2)
    characters_empty = round((100 - progressbar_percent) / 2)

    delta_time = round(time.time() - start_time)
    minutes = math.floor(delta_time / 60)
    seconds = delta_time - (minutes * 60)

    print(
        f"[{characters_filled * '=' + characters_empty * ' '}] {round(progressbar_percent)}% ({completed} / {drill_amount} completed) in {str(minutes).zfill(2)}:{str(seconds).zfill(2)} ",
        end="\r")

    return percentage


class Drill:
    start_percentage = -1
    percentage = -1
    reference = None

    def __init__(self, drill_id):
        self.__dict__ = requests.get(f"https://www.drillster.com/api/2.1.1/playable/{drill_id}", headers=header).json()
        thread = threading.Thread(target=self.start_drill)
        thread.start()

    def start_drill(self):
        # Load existing dictionary of questions and answers from file if it exists, otherwise create empty dictionary
        wordlist = wordlists[self.id] if self.id in wordlists else {}

        while self.percentage < 100:
            # Get a question from the drill
            question_object = self.get_question()
            question = question_object["ask"]["term"]["value"]
            column = question_object["tell"]["name"]
            answer_object: json

            # Check if the question is already in the dictionary of questions and answers
            if (question not in wordlist) or (column not in wordlist[question]):
                # Answer the question and add the question-answer pair to the dictionary
                answer_object = self.answer_question(answer="")
            else:
                # Answer the question using the previously recorded answer from the dictionary
                answer_object = self.answer_question(answer=wordlist[question][column])

            # First set the answer, after setting check if the answer is correct - Drills could change
            if answer_object["evaluation"]["result"] == "INCORRECT":
                # Get all the possible answers
                correct_answers = answer_object["evaluation"]["termEvaluations"]

                # Check for multiple answers - store them all
                if question_object["tell"]["composition"] != "SET":
                    # Store the only correct answer
                    store_answer = correct_answers[1]["value"]
                else:
                    store_answer = []

                    # Store all possible answers
                    for answer in correct_answers:
                        if answer["value"] != "":
                            store_answer.append(answer["value"])

                    store_answer = correct_answers

                if question not in wordlist:
                    wordlist[question] = {}

                wordlist[question][column] = store_answer

        wordlists[self.id] = wordlist

    def get_question(self):
        try:
            question = requests.get(f"https://www.drillster.com/api/2.1.1/question/{self.id}", headers=header).json()
            question_object = question["question"]
            self.reference = question_object["reference"]
            return question_object
        except ConnectTimeout:
            time.sleep(1)
            return self.get_question()

    def answer_question(self, answer):
        answer_response: json
        send_question_data: json

        if isinstance(answer, str):
            send_question_data = {"answer": answer}
        else:
            send_question_data = []

            for index in answer:
                send_question_data.append(("answer", index))

        try:
            answer_response = requests.put(f"https://www.drillster.com/api/2.1.1/answer/{self.reference}",
                                           headers=header, data=send_question_data).json()
            self.percentage = answer_response["proficiency"]["overall"]

            if self.start_percentage == -1:
                self.start_percentage = self.percentage

            return answer_response
        except ConnectTimeout:
            time.sleep(1)
            return self.answer_question(answer)
