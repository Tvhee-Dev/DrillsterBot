//Drill iFrame script - answering questions and learning values - communicating with background.js
//Storage
let enableStorage = undefined;
let autoClose = undefined;
let answerTime = undefined;
let flawMarge = undefined;
let wordlist = undefined;

//Runtime
let frameRefreshLoopId = undefined;
let currentDrillId = undefined;
let columnAsked = false;
let mistakes = 0;
let questionsTaken = 0;

//Timing
let frameRefreshSpeed = -1;

async function start() {
    try {
        if (window.self === window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
        if (request.set_answer_time !== undefined)
            answerTime = request.set_answer_time;
        else if (request.set_storage_enabled !== undefined)
            enableStorage = request.set_storage_enabled;
        else if (request.set_auto_close !== undefined)
            autoClose = request.set_auto_close;
        else if (request.set_flaw_marge !== undefined)
            flawMarge = request.set_flaw_marge;

        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON")
                setTimeout(() => startPlaying(), 1000);
            else if (request.run_mode === "OFF")
                stopPlaying();
        }

        if (request.show_wordlist !== undefined)
            wordlistToMessage();

        sendResponse();
        return true;
    });

    chrome.runtime.sendMessage({get_storage: true}, (response) => {
        enableStorage = response.storage_enabled;
        answerTime = response.answer_time;
        flawMarge = response.flaw_marge;
        autoClose = response.auto_close;
    });

    chrome.runtime.sendMessage({off_state: true}).then();
    currentDrillId = window.location.href.split("https://www.drillster.com/widgets/player/4/")[1].substring(0, 22);
    wordlist = getWordlist(currentDrillId);
}

function startPlaying() {
    if (document.getElementsByClassName("details-page__scrim")[0] === undefined) {
        startQuestionLoop();
        return;
    }

    let startButtonInterval = setInterval(() => {
        let startButton = document.getElementsByClassName("dwc-button dwc-button--contained")[0];

        if (startButton !== undefined) {
            startButton.click();
            clearInterval(startButtonInterval);

            let startInterval = setInterval(() => {
                if (document.getElementsByClassName("details-page__scrim")[0] !== undefined)
                    return;

                clearInterval(startInterval);
                startQuestionLoop();
            }, 1);
        }
    }, 1);
}

function startQuestionLoop() {
    function renderFrame() {
        //Protection: The loop can sometimes run another time AFTER stopPlaying() was called because of the 10 ms
        if(frameRefreshLoopId === -1)
            return;

        const now = new Date().getTime();

        let percentage = 100;
        let percentageElement = document.getElementsByClassName("dwc-proficiency-meter__percentage")[0];

        if (percentageElement === undefined && frameRefreshSpeed < 0)
            return;

        if (percentageElement !== undefined)
            percentage = parseInt(percentageElement.innerText.replaceAll("%", ""));

        if (percentage === 100) {
            try {
                document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
            } catch (e) {
                return;
            }

            clearInterval(frameRefreshLoopId);
            frameRefreshLoopId = undefined;
            stopPlaying();

            let titleInterval = setInterval(() => {
                let drillTitle = document.getElementsByClassName("playable-card__details--name dwc-typography--body2")[0];

                if (drillTitle === undefined)
                    return;

                clearInterval(titleInterval);

                chrome.runtime.sendMessage({
                    percentage: 100,
                    questions: questionsTaken,
                    flaws: mistakes,
                    drill_title: drillTitle.innerText,
                    auto_close: autoClose
                }).then();

                document.getElementsByClassName("dwc-icon-button dwc-icon-button--secondary")[0].click();
            }, 1);
            return;
        }

        if (document.getElementById("loadindicator") === null) {
            if (frameRefreshSpeed < 0)
                frameRefreshSpeed = now;

            if (now - frameRefreshSpeed > 0 && document.getElementById("drillsterbot-speed") === null) {
                let paragraph = document.getElementsByClassName("playable-frame__header--left")[0];
                let element = document.createElement("div");
                let text = document.createTextNode("Snelheid: " + (now - frameRefreshSpeed) + " ms / frame");

                frameRefreshSpeed = now;

                element.id = "drillsterbot-speed";
                element.className = "playable-frame__header--right";
                element.appendChild(text);
                paragraph.appendChild(element);
            }

            let sampleElement = document.createElement("p");
            //Add sample element - check if removed to indicate new page
            //Only work with text fields, get the last one
            sampleElement.id = "loadindicator";

            let inputField = document.getElementsByClassName("dwc-text-field__input")[0];

            if(inputField !== undefined)
                inputField.appendChild(sampleElement);
            else
                document.getElementsByClassName("dwc-markup-text")[0].appendChild(sampleElement);

            document.getElementsByClassName("dwc-icon-button dwc-icon-button--secondary")[0].addEventListener("click", () => {
                stopPlaying();
                chrome.runtime.sendMessage({off_state: true}).then();
            });

            setTimeout(() => {
                if (document.getElementsByClassName("drl-feedback__header__value").length > 0)
                    document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
                else if (document.getElementsByClassName("drl-introduction__ask__name")[0] !== undefined)
                    retrieveAnswer();
                else if (document.getElementsByClassName("question-component question-component--evaluated")[0] !== undefined)
                    learnAnswer();
                else
                    setAnswer();
            }, answerTime);
        }
    }

    frameRefreshLoopId = setInterval(() => renderFrame(), 10); //1 ms is way too fast for the code to run smoothly
}

function stopPlaying() {
    if (frameRefreshLoopId !== undefined)
        clearInterval(frameRefreshLoopId);

    frameRefreshSpeed = -1;
    frameRefreshLoopId = undefined;
}

function setAnswer() {
    let questionObject = document.getElementsByClassName("question-component__ask__term")[0];
    let columnObject = document.getElementsByClassName("question-component__tell__name")[0];
    let question;

    if (columnObject !== undefined) {
        if (!columnAsked)
            columnAsked = true;

        question = columnObject.innerText.toLowerCase() + "\\" + questionObject.innerText;
    } else {
        question = questionObject.innerText;
    }

    if (wordlist[question] !== undefined) {
        let answerToSet = wordlist[question];

        if ((mistakes / questionsTaken) < (flawMarge / 100))
            answerToSet = "Foutmarge";

        if (document.getElementsByClassName("multiple-choice-input__frame")[0] !== undefined)
            setMultipleChoiceAnswer(answerToSet);
        else
            setOpenAnswer(answerToSet);
    } else {
        retrieveAnswer();
    }
}

function setOpenAnswer(value) {
    let inputField = document.getElementsByClassName("dwc-text-field__input")[0];

    inputField.value = value === undefined || value === "" ? "a" : value.split("+")[0];
    inputField.dispatchEvent(new KeyboardEvent("keydown", {"key": "a"}));
    inputField.dispatchEvent(new KeyboardEvent("keyup", {"key": "a"}));

    setTimeout(() => {
        document.getElementsByClassName("drl-enlarged-button")[0].click();

        if(value === "Foutmarge")
            mistakes++;
        else
            questionsTaken++;
    }, 1);
}

function setMultipleChoiceAnswer(value) {
    let values = (value === undefined ? "" : value).split("+");
    let multipleChoiceTable = document.getElementsByClassName("dwc-markup-text");
    let answerButtonSelectionChoice = document.getElementsByClassName("drl-enlarged-button")[0];
    let answerButtonPresent = answerButtonSelectionChoice !== undefined;
    let foundAnyAnswer = false;

    for (let i = 1 /*1, because it contains the question*/; i < multipleChoiceTable.length; i++) {
        let answerButton = multipleChoiceTable[i];

        for (let j = 0; j < values.length; j++) {
            if ((value === "Foutmarge" && answerButton.innerText !== values[j]) ||
                (value !== "Foutmarge" && answerButton.innerText === values[j])) {
                answerButton.click();
                foundAnyAnswer = true;
            }
        }
    }

    if (!foundAnyAnswer) {
        retrieveAnswer();
        return;
    }

    setTimeout(function () {
        if (answerButtonPresent)
            answerButtonSelectionChoice.click();

        if(value === "Foutmarge")
            mistakes++;
        else
            questionsTaken++;
    }, 1);
}

function retrieveAnswer() {
    let idkButton = document.getElementsByClassName("question-component__button question-component__button--dontknow")[0];

    if (idkButton !== undefined) {
        idkButton.click();
        return;
    }

    let columnObject = document.getElementsByClassName("drl-introduction__tell__name")[0];
    let answerObjects = document.getElementsByClassName("dwc-markup-text");
    let column = columnObject === undefined ? undefined : columnObject.innerText;
    let question = answerObjects[0].innerText;
    let answer = "";

    for (let i = 1 /*1, because it contains the question*/; i < answerObjects.length; i++)
        answer = answer + (answer.length > 0 ? "+" : "") + answerObjects[i].innerText;

    if (columnAsked && columnObject !== undefined)
        wordlist[column.toLowerCase() + "\\" + question] = answer;
    else
        wordlist[question] = answer;

    if (enableStorage)
        saveWordlist(currentDrillId);

    document.getElementsByClassName("dwc-button")[0].click();
}

function learnAnswer() {
    let correctAnswers = document.getElementsByClassName("dwc-markup-text");
    let question = correctAnswers[0];
    let correctAnswerList = "";

    for (let i = 1; i < correctAnswers.length - 1; i++) {
        let correctAnswer = correctAnswers[i];
        let answer = correctAnswer.innerText;
        correctAnswerList = correctAnswerList + (correctAnswerList.length > 0 ? "+" : "") + answer;
    }

    wordlist[question.innerText] = correctAnswerList;

    if (enableStorage)
        saveWordlist(currentDrillId);

    document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
}

//Storage
function saveCookieLocal(name, text) {
    localStorage.setItem("DrillsterBot_" + encodeURIComponent(name), encodeURIComponent(text));
}

function getCookieLocal(name, defaultValue) {
    let cookieName = "DrillsterBot_" + encodeURIComponent(name);
    let result = localStorage.getItem(cookieName);

    if (result === null)
        return defaultValue;

    return decodeURIComponent(result);
}

function getWordlist(drillTitle) {
    const wordlist = {};
    let title = drillTitle.replaceAll(" ", "_");
    let cookieIndex = 1;
    let cookieText;

    while ((cookieText = getCookieLocal("Wordlist_" + title + "_" + cookieIndex, "")) !== "") {
        const wordsSplit = cookieText.split("|");

        for (let i = 0; i < wordsSplit.length; i++) {
            let dictionaryValue = wordsSplit[i];
            let question = dictionaryValue.split("=")[0];
            wordlist[question] = dictionaryValue.split("=")[1];
        }

        cookieIndex++;
    }

    return wordlist;
}

function saveWordlist(drillTitle) {
    const header = {};
    let text = "";

    for (let [question, answer] of Object.entries(wordlist)) {
        let currentQuestion = question + "=" + answer;
        text = text + (text.length === 0 ? "" : "|") + currentQuestion;
    }

    let headerString = "";

    for (let [key, number] of Object.entries(header)) {
        if (headerString.length === 0)
            headerString = key + "=" + number;
        else
            headerString = headerString + "|" + key + "=" + number;
    }

    if (headerString.length !== 0)
        headerString = headerString + "<>";

    let completedText = headerString + text;
    let cookieSplit = completedText.match(/.{1,3800}/g);

    for (let i = 0; i < cookieSplit.length; i++)
        saveCookieLocal("Wordlist_" + drillTitle.replaceAll(" ", "_") + "_" + (i + 1), cookieSplit[i]);
}

function wordlistToMessage() {
    let words = "";
    let byCategory = {};

    for (let [question, answer] of Object.entries(wordlist)) {
        let categorySplit = question.split("\\");
        let category = categorySplit.length > 1 ? categorySplit[0] : "Default";
        let word = categorySplit[categorySplit.length > 1 ? 1 : 0];
        let wordsOfCategory = byCategory[category];

        if (wordsOfCategory === undefined)
            wordsOfCategory = [];

        wordsOfCategory[wordsOfCategory.length] = word + " = " + answer.replaceAll("+", " + ");
        byCategory[category] = wordsOfCategory;
    }

    let categories = Object.keys(byCategory);
    categories.sort();

    for (let i = 0; i < categories.length; i++) {
        let category = categories[i];
        let wordlist = byCategory[category];

        wordlist.sort();
        words = (words.length > 0 ? words + "\n" : words) + "Categorie: " + category + "\n";

        for (let j = 0; j < wordlist.length; j++)
            words = words + wordlist[j] + "\n";
    }

    navigator.clipboard.writeText(words).then(() =>
        alert("Woordenlijst van " + currentDrillId + " is gekopieerd naar het klembord!"));
}

start();