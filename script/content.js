//Storage
let enableStorage = undefined;
let autoClose = undefined;
let answerTime = undefined;
let flawMarge = undefined;
let wordlist = undefined;

//Runtime
let lastQuestionTime = new Date().getTime();
let currentDrill = undefined;
let columnAsked = false;
let mistakes = 0;
let questionsTaken = 0;

function start() {
    try {
        if (window.self === window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
        if (request.storage !== undefined) {
            sendResponse({
                storage_enabled: enableStorage,
                answer_time: answerTime,
                flaw_marge: flawMarge,
                auto_close: autoClose
            });
            return true;
        }

        if (request.set_answer_time !== undefined) {
            saveCookie("Answer_Delay", request.set_answer_time);
            answerTime = request.set_answer_time;
        }

        if (request.set_storage_enabled !== undefined) {
            saveCookie("Storage_Enabled", request.set_storage_enabled);
            enableStorage = request.set_storage_enabled;
        }

        if (request.set_auto_close !== undefined) {
            saveCookie("Auto_Close", request.set_auto_close);
            autoClose = request.set_auto_close;
        }

        if (request.set_flaw_marge !== undefined) {
            saveCookie("Flaw_Marge", request.set_flaw_marge);
            flawMarge = request.set_flaw_marge;
        }

        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON") {
                prepareForDrill();
                main();
            } else if (request.run_mode === "OFF") {
                currentDrill = undefined;
            }

            sendResponse({running: currentDrill !== undefined});
        }

        if (request.show_wordlist !== undefined) {
            prepareForDrill();
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

                for (let j = 0; j < wordlist.length; j++) {
                    words = words + wordlist[j] + "\n";
                }
            }

            await navigator.clipboard.writeText(words);
            alert("Woordenlijst van " + currentDrill + " is gekopieerd naar het klembord!");
        }

        sendResponse();
        return true;
    });

    setTimeout(function () {
        enableStorage = getCookie("Storage_Enabled", "true") === "true";
        autoClose = getCookie("Auto_Close", "true") === "true";
        answerTime = parseInt(getCookie("Answer_Delay", "100"));
        flawMarge = parseInt(getCookie("Flaw_Marge", "0"));

        if (flawMarge > 99 || flawMarge < 0)
            flawMarge = 25;

        if (answerTime > 999 || answerTime < 100)
            answerTime = 500;

        chrome.runtime.sendMessage({initialize: true}).then();
    }, 10);
}

function main() {
    if (currentDrill === undefined)
        return;

    const now = new Date().getTime();
    console.log("Question Delay: " + (now - lastQuestionTime) + " ms");
    lastQuestionTime = now;

    if (getPercentage() === 100) {
        chrome.runtime.sendMessage({
            percentage: 100,
            questions: questionsTaken,
            flaws: mistakes,
            drill_title: currentDrill,
            auto_close: autoClose
        }).then();
        currentDrill = undefined;
        return;
    }

    let flipText = document.getElementsByClassName("drl-introduction__helper-text")[0];

    if (flipText !== undefined) {
        retrieveAnswer();
    } else {
        setAnswer();
    }
}

function setAnswer() {
    let questionObject = document.getElementsByClassName("question-component__ask__term")[0];
    let columnObject = document.getElementsByClassName("question-component__tell__name")[0];

    let question = tryAction(() => {
        if (columnObject !== undefined) {
            if (!columnAsked)
                columnAsked = true;

            return columnObject.innerText.toLowerCase() + "\\" + questionObject.innerText;
        }

        return questionObject.innerText;
    });

    if (question === undefined)
        return;

    if (wordlist[question] !== undefined) {
        let answerToSet = wordlist[question];
        questionsTaken++;

        if ((mistakes / questionsTaken) < (flawMarge / 100)) {
            answerToSet = "Foutmarge";
            mistakes++;
        }

        let open = document.getElementsByClassName("multiple-choice-input__frame")[0] === undefined;

        if(!(open ? setOpenAnswer(answerToSet) : setMultipleChoiceAnswer(answerToSet))) {
            return;
        }

        setTimeout(function () {
            let correctAnswers = document.getElementsByClassName("drl-term drl-term--disabled");

            if(correctAnswers.length > 0) {
                let correctAnswerList = "";

                for (let i = 0; i < correctAnswers.length; i++) {
                    let correctAnswer = correctAnswers[i];

                    if(correctAnswer.className !== "drl-term drl-term--disabled")
                        continue;

                    let answer = correctAnswer.innerText;
                    correctAnswerList = correctAnswerList + (correctAnswerList.length > 0 ? "+" : "") + answer;
                }

                wordlist[question] = correctAnswerList;

                if (enableStorage)
                    saveWordlist(currentDrill);

                document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
                setTimeout(main, answerTime);
            } else {
                main();
            }
        }, answerTime);
    } else {
        retrieveAnswer();
    }
}

function setOpenAnswer(value) {
    try {
        let inputField = document.getElementsByClassName("dwc-text-field__input")[0];
        let submitButton = document.getElementsByClassName("drl-enlarged-button")[0];

        pressSampleKey(inputField);
        inputField.value = value === undefined ? "a" : value;
        submitButton.click();
        return true;
    } catch (error) {
        setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
        return false;
    }
}

function setMultipleChoiceAnswer(value) {
    let values = (value === undefined ? "" : value).split("+");

    try {
        let multipleChoiceTable = document.getElementsByClassName("dwc-markup-text");
        let answerButtonSelectionChoice = document.getElementsByClassName("drl-enlarged-button")[0];
        let answerButtonPresent = answerButtonSelectionChoice !== undefined;
        let foundAnyAnswer = false;

        for(let i = 1 /*1, because it contains the question*/; i < multipleChoiceTable.length; i++) {
            let answerButton = multipleChoiceTable[i];

            for (let j = 0; j < values.length; j++) {
                if((value === "Foutmarge" && answerButton.innerText !== values[j]) ||
                    (value !== "Foutmarge" && answerButton.innerText === values[j])) {
                    answerButton.click();
                    foundAnyAnswer = true;
                }
            }
        }

        if(!foundAnyAnswer) {
            retrieveAnswer();
            return false;
        }

        setTimeout(function () {
            if(answerButtonPresent)
                answerButtonSelectionChoice.click();
        }, 1);

        return true;
    } catch (error) {
        setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
        return false;
    }
}

function retrieveAnswer() {
    let idkButton = document.getElementsByClassName("question-component__button question-component__button--dontknow")[0];

    if (idkButton !== undefined) {
        try {
            idkButton.click();
        } catch (error) {
            console.log("Could not find element. Retrying in 50ms");
            setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
            return;
        }
    }

    setTimeout(function () {
        let columnObject = document.getElementsByClassName("drl-introduction__tell__name")[0];
        let answerObjects = document.getElementsByClassName("dwc-markup-text");

        let column = tryAction(() => {
            return columnObject === undefined ? undefined : columnObject.innerText
        });

        let question = tryAction(() => {
            return answerObjects[0].innerText
        });

        let answer = tryAction(() => {
            let answers = "";

            for (let i = 1 /*1, because it contains the question*/; i < answerObjects.length; i++) {
                answers = answers + (answers.length > 0 ? "+" : "") + answerObjects[i].innerText;
            }

            return answers;
        });

        if (question === undefined || answer === undefined)
            return;

        if (columnAsked && columnObject !== undefined)
            wordlist[column.toLowerCase() + "\\" + question] = answer;
        else
            wordlist[question] = answer;

        if (enableStorage)
            saveWordlist(currentDrill);

        try {
            document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
            setTimeout(main, answerTime);
        } catch (error) {
            console.log("Could not find element. Retrying in 50ms");
            setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
        }
    }, answerTime);
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

    for (let i = 0; i < cookieSplit.length; i++) {
        saveCookie("Wordlist_" + drillTitle.replaceAll(" ", "_") + "_" + (i + 1), cookieSplit[i]);
    }
}

function prepareForDrill() {
    let title = document.getElementsByClassName("playable-frame__header__title")[0];

    if (title === undefined)
        return;

    currentDrill = title.innerText;
    wordlist = getWordlist(currentDrill);
}

//Util
function tryAction(action) {
    try {
        return action();
    } catch (error) {
        console.log("Could not find element. Retrying in 50ms");
        setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
    }
}

function pressSampleKey(inputField) {
    try {
        inputField.dispatchEvent(new KeyboardEvent("keydown", {"key": "a"}));
        inputField.dispatchEvent(new KeyboardEvent("keyup", {"key": "a"}));
    } catch (error) {
        console.log("Could not find element. Retrying in 50ms");
        setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
    }
}

function getPercentage() {
    let percentage = document.getElementsByClassName("dwc-proficiency-meter__percentage")[0];
    return percentage === undefined ? 100 : parseInt(percentage.innerText.replaceAll("%", ""));
}

start();
