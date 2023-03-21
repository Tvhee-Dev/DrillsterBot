import requests
import json

header: json


def set_token(drillster_token):
    global header
    header = {"Authorization": f"Bearer {drillster_token}"}
    return


def get_repertoire():
    return requests.get("https://www.drillster.com/api/3/repertoire",
                        headers=header).json()["playableRenditions"]


def get_course_content(playable):
    return requests.get(f"https://www.drillster.com/api/3/results?playable={playable}",
                        headers=header).json()["results"]


class Drill:
    def __init__(self, drill_id):
        self.reference = None
        self.__dict__ = requests.get(f"https://www.drillster.com/api/2.1.1/playable/{drill_id}", headers=header).json()

    def get_question(self):
        question = requests.get(f"https://www.drillster.com/api/2.1.1/question/{self.id}", headers=header).json()
        question_object = question["question"]
        self.reference = question_object["reference"]
        return question_object

    def answer_question(self, answer):
        answer_response = requests.put(f"https://www.drillster.com/api/2.1.1/answer/{self.reference}",
                                       headers=header, data={"answer": answer}).json()
        return answer_response
