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

    chrome.runtime.onMessage.addListener(function (request, sender, sendResponse) {
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
                currentDrill = document.getElementsByClassName("playable-frame__header__title")[0].innerText;
                main();
            } else if (request.run_mode === "OFF") {
                currentDrill = undefined;
            }

            sendResponse({running: currentDrill !== undefined});
        }

        sendResponse();
        return true;
    });

    setTimeout(function () {
        enableStorage = getCookie("Storage_Enabled", "true") === "true";
        autoClose = getCookie("Auto_Close", "true") === "true";
        answerTime = parseInt(getCookie("Answer_Delay", "400"));
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

    if (wordlist === undefined)
        wordlist = getWordlist(currentDrill);

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
    let inputField = document.getElementsByClassName("dwc-text-field__input")[0];
    let submitButton = document.getElementsByClassName("drl-enlarged-button")[0];

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
        pressSampleKey(inputField);
        questionsTaken++;

        try {
            if ((mistakes / questionsTaken) < (flawMarge / 100)) {
                inputField.value = "Flaw Marge";
                mistakes++;
            } else {
                inputField.value = wordlist[question];
            }
        }
        catch (error) {
            console.log("Could not find element. Retrying in 50ms");
            setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
            return;
        }

        setTimeout(function () {
            submitButton.click();

            setTimeout(function () {
                let correctAnswer = document.getElementsByClassName("drl-term drl-term--open-ended")[0];

                if ((wordlist[question] === undefined) || (correctAnswer !== undefined)) {
                    wordlist[question] = correctAnswer.innerText;

                    if (enableStorage)
                        saveWordlist(currentDrill);

                    document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
                    setTimeout(main, answerTime);
                } else {
                    main();
                }
            }, answerTime);
        }, 1);
    } else {
        retrieveAnswer();
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
        let questionObject = document.getElementsByClassName("dwc-markup-text")[0];
        let answerObject = document.getElementsByClassName("dwc-markup-text")[1];

        let column = tryAction(() => {
            return columnObject === undefined ? undefined : columnObject.innerText
        });
        let question = tryAction(() => {
            return questionObject.innerText
        });
        let answer = tryAction(() => {
            return answerObject.innerText
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

    let headerString = ""

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
    }
    catch (error) {
        console.log("Could not find element. Retrying in 50ms");
        setTimeout(async () => await chrome.runtime.sendMessage({retry: true}), 50);
    }
}

function getPercentage() {
    let percentage = document.getElementsByClassName("proficiency-meter__percentage")[0];
    return percentage === undefined ? 100 : parseInt(percentage.innerText.replaceAll("%", ""));
}

start();