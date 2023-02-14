//Storage
let enableStorage = undefined;
let autoClose = undefined;
let answerTime = undefined;
let flawMarge = undefined;
let wordlist = undefined;

//Runtime
let frameRefreshLoopId = undefined;
let currentDrill = undefined;
let columnAsked = false;
let mistakes = 0;
let questionsTaken = 0;

//Timing
let startPercentage = -1;
let startPercentageTime = -1;

function start() {
    try {
        if (window.self === window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
        if (request.storage !== undefined) {
            sendResponse({storage_enabled: enableStorage, answer_time: answerTime,
                flaw_marge: flawMarge, auto_close: autoClose});
            return true;
        }

        if (request.set_answer_time !== undefined)
            saveCookie("Answer_Delay", (answerTime = request.set_answer_time));

        if (request.set_storage_enabled !== undefined)
            saveCookie("Storage_Enabled", (enableStorage = request.set_storage_enabled));

        if (request.set_auto_close !== undefined)
            saveCookie("Auto_Close", (autoClose = request.set_auto_close));

        if (request.set_flaw_marge !== undefined)
            saveCookie("Flaw_Marge", (flawMarge = request.set_flaw_marge));

        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON")
                startPlaying();
            else if (request.run_mode === "OFF")
                stopPlaying();

            sendResponse({running: currentDrill !== undefined});
        }

        if (request.show_wordlist !== undefined)
            wordlistToMessage();

        sendResponse();
        return true;
    });

    setTimeout(function () {
        enableStorage = getCookie("Storage_Enabled", "true") === "true";
        autoClose = getCookie("Auto_Close", "true") === "true";
        answerTime = parseInt(getCookie("Answer_Delay", "1"));
        flawMarge = parseInt(getCookie("Flaw_Marge", "0"));

        if (flawMarge > 99 || flawMarge < 0)
            flawMarge = 0;

        if (answerTime > 999 || answerTime < 1)
            answerTime = 1;

        chrome.runtime.sendMessage({initialize: true}).then();
    }, 1);

    let titleLoader = setInterval(() => {
        let drillTitle = document.getElementsByClassName("details-page__cover__title")[0];

        if (drillTitle !== undefined) {
            currentDrill = drillTitle.innerText;
            wordlist = getWordlist(currentDrill);
            clearInterval(titleLoader);
        }
    }, 50);
}

function startPlaying() {
    frameRefreshLoopId = setInterval(() => {
        const now = new Date().getTime();
        let percentageElement = document.getElementsByClassName("dwc-proficiency-meter__percentage")[0];
        let percentage = 100;

        if(percentageElement !== undefined)
            percentage = parseInt(percentageElement.innerText.replaceAll("%", ""));

        if (percentage === 100 || currentDrill === undefined) {
            chrome.runtime.sendMessage({percentage: 100, questions: questionsTaken, flaws: mistakes,
                drill_title: currentDrill, auto_close: autoClose}).then();

            stopPlaying();
            return;
        }

        if (document.getElementsByClassName("loadindicator").length === 0) {
            if(startPercentage < 0 || startPercentageTime < 0) {
                startPercentage = percentage;
                startPercentageTime = now;
            }

            let dPercentage = percentage - startPercentage;

            if(dPercentage > 1) {
                let dTime = (now - startPercentageTime) / 1000;
                let eta = (dTime / dPercentage) * (100 - percentage);
                eta = Math.round(flawMarge === 0 ? eta : (eta * (1 / flawMarge)));

                let paragraph = document.getElementsByClassName("playable-frame__header--left")[0];
                let element = document.createElement("div");
                let text = document.createTextNode("ETA: " + eta + " seconde(n)");

                element.id = "drillsterbot-eta";
                element.className = "playable-frame__header--right";
                element.appendChild(text);
                paragraph.appendChild(element);
            }

            let sampleElement = document.createElement("p");
            //Add sample element - check if removed to indicate new page
            sampleElement.className = "loadindicator";
            document.getElementsByClassName("dwc-button")[0].appendChild(sampleElement);

            //Check what to do, 3 possibilities: retrieve, learn and set
            setTimeout(() => {
                if (document.getElementsByClassName("drl-introduction__ask__name")[0] !== undefined)
                    retrieveAnswer();
                else if (document.getElementsByClassName("question-component question-component--evaluated")[0] !== undefined)
                    learnAnswer();
                else
                    setAnswer();
            }, 1);
        }
    }, answerTime);
}

function stopPlaying() {
    if (frameRefreshLoopId !== undefined)
        clearInterval(frameRefreshLoopId);

    startPercentage = -1;
    startPercentageTime = -1;
    currentDrill = undefined;
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
        questionsTaken++;

        if ((mistakes / questionsTaken) < (flawMarge / 100)) {
            answerToSet = "Foutmarge";
            mistakes++;
        }

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

    setTimeout(() => document.getElementsByClassName("drl-enlarged-button")[0].click(), 1);
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
        saveWordlist(currentDrill);

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
        saveWordlist(currentDrill);

    document.getElementsByClassName("dwc-button")[0].click();
}

//Storage
function saveCookie(name, text) {
    localStorage.setItem("DrillsterBot_" + encodeURIComponent(name), encodeURIComponent(text));
}

function getCookie(name, defaultValue) {
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

    while ((cookieText = getCookie("Wordlist_" + title + "_" + cookieIndex, "")) !== "") {
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
        saveCookie("Wordlist_" + drillTitle.replaceAll(" ", "_") + "_" + (i + 1), cookieSplit[i]);
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
        alert("Woordenlijst van " + currentDrill + " is gekopieerd naar het klembord!"));
}

start();