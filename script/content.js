//Storage
let enableStorage = undefined;
let answerTime = undefined;
let flawMarge = undefined;
let wordlist = undefined;

//Runtime
let running = false;
let tabId;
let columnAsked = false;
let mistakes = 0;
let questionsTaken = 0;

function load() {
    try {
        if (window.self === window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener(function (request, sender, sendResponse) {
        if (request.storage !== undefined) {
            sendResponse({storage_enabled: enableStorage, answer_time: answerTime, flaw_marge: flawMarge});
            return true;
        }

        if (request.tabId !== undefined)
            tabId = request.tabId;

        if (request.set_answer_time !== undefined) {
            saveCookie("Answer_Delay", request.set_answer_time, 180);
            answerTime = request.set_answer_time;
        }

        if (request.set_storage_enabled !== undefined) {
            saveCookie("Storage_Enabled", request.set_storage_enabled, 180);
            enableStorage = request.set_storage_enabled;
        }

        if (request.set_flaw_marge !== undefined) {
            saveCookie("Flaw_Marge", request.set_flaw_marge, 180);
            flawMarge = request.set_flaw_marge;
        }

        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON") {
                running = true;
                main();
            } else if (request.run_mode === "OFF") {
                running = false;
            }

            sendResponse({running: running});
        }

        sendResponse();
        return true;
    });

    setTimeout(function () {
        enableStorage = getCookie("Storage_Enabled", "true") === "true";
        answerTime = parseInt(getCookie("Answer_Delay", "450"));
        flawMarge = parseInt(getCookie("Flaw_Marge", "0"));

        if (flawMarge > 99 || flawMarge < 0)
            flawMarge = 25;

        if (answerTime > 999 || answerTime < 100)
            answerTime = 500;
    }, 10);
}

function main() {
    if (!running) {
        return;
    }

    if (wordlist === undefined)
        wordlist = getWordlist(document.getElementsByClassName("playable-frame__header__title")[0].innerText);

    if (getPercentage() === 100) {
        chrome.runtime.sendMessage({percentage: 100, questions: questionsTaken, flaws: mistakes, tabId: tabId}).then();
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
    let question = document.getElementsByClassName("question-component__ask__term")[0].innerText;
    let column = document.getElementsByClassName("question-component__tell__name")[0];
    let inputField = document.getElementsByClassName("dwc-text-field__input")[0];
    let submitButton = document.getElementsByClassName("drl-enlarged-button")[0];

    if (column !== undefined) {
        question = column.innerText.toLowerCase() + "\\" + question;

        if (!columnAsked)
            columnAsked = true;
    }

    if (wordlist[question] !== undefined) {
        pressSampleKey(inputField);
        questionsTaken++;

        if ((mistakes / questionsTaken) < (flawMarge / 100)) {
            inputField.value = "Flaw Marge"
            mistakes++;
        } else {
            inputField.value = wordlist[question];
        }

        setTimeout(function () {
            submitButton.click();

            setTimeout(function () {
                let correctAnswer = document.getElementsByClassName("drl-term drl-term--open-ended")[0];

                if ((wordlist[question] === undefined) || (correctAnswer !== undefined)) {
                    wordlist[question] = correctAnswer.innerText;

                    if (enableStorage)
                        saveWordlist(document.getElementsByClassName("playable-frame__header__title")[0].innerText);

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

    if (idkButton !== undefined)
        idkButton.click();

    setTimeout(function () {
        let column = document.getElementsByClassName("drl-introduction__tell__name")[0];
        let question = document.getElementsByClassName("dwc-markup-text")[0];
        let answer = document.getElementsByClassName("dwc-markup-text")[1];

        if (columnAsked)
            wordlist[column.innerText.toLowerCase() + "\\" + question.innerText] = answer.innerText;
        else
            wordlist[question.innerText] = answer.innerText;

        document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();

        if (enableStorage)
            saveWordlist(document.getElementsByClassName("playable-frame__header__title")[0].innerText);

        setTimeout(main, answerTime);
    }, answerTime);
}

//Storage
function saveCookie(name, text, days) {
    let date = new Date();
    date.setTime(date.getTime() + (days * 24 * 3600 * 1000));
    document.cookie = "DrillsterBot_" + encodeURIComponent(name) + "=" + encodeURIComponent(text) + ";expires=" + date.toUTCString() + ";path=/";
}

function getCookie(name, defaultValue) {
    let cookieName = "DrillsterBot_" + name + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(";");
    let result = "";

    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];

        while (c.charAt(0) === " ") {
            c = c.substring(1);
        }

        if (c.indexOf(cookieName) === 0) {
            result = c.substring(cookieName.length, c.length);
        }
    }

    if (result === "")
        return defaultValue;

    return result;
}

function getWordlist(drillTitle) {
    const wordlist = {};
    let title = drillTitle.replaceAll(" ", "_");
    let cookieIndex = 1;
    let cookieText;

    while ((cookieText = getCookie("Wordlist_" + title + "_" + cookieIndex, "")) !== "") {
        /*const header = cookieText.toString().includes("<>") ? cookieText.toString().split("<>")[0] : "";
        const words = cookieText.toString().includes("<>") ? cookieText.toString().split("<>")[1] : cookieText;

        for (let repetitiveValue in header.split("|")) {
            let key = repetitiveValue.split("=")[1];
            let value = repetitiveValue.split("=")[0];

            words.replaceAll(key, value);
        }*/

        const wordsSplit = cookieText.split("|");//words.split("|");

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
    //let headerCount = 1;
    const header = {};
    let text = "";

    for (let [question, answer] of Object.entries(wordlist)) {
        let currentQuestion = question + "=" + answer;

        /*if(currentQuestion.includes("\\")) {
            let keyToReplace = currentQuestion.split("\\")[0];
            let headerNumber = header[keyToReplace];

            if(headerNumber === undefined) {
                headerNumber = "^" + headerCount;
                header[keyToReplace] = headerNumber;
                headerCount++;
            }

            currentQuestion = currentQuestion.replace(keyToReplace + "\\", headerNumber + "\\");
        }*/

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
        saveCookie("Wordlist_" + drillTitle.replaceAll(" ", "_") + "_" + (i + 1), cookieSplit[i], 180);
    }
}

//Util
function pressSampleKey(inputField) {
    inputField.dispatchEvent(new KeyboardEvent("keydown", {"key": "a"}));
    inputField.dispatchEvent(new KeyboardEvent("keyup", {"key": "a"}));
}

function getPercentage() {
    let percentage = document.getElementsByClassName("proficiency-meter__percentage")[0];
    return percentage === undefined ? 100 : parseInt(percentage.innerText.replaceAll("%", ""));
}

load();