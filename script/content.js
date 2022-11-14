//Storage
let enableStorage = undefined;
let answerTime = undefined;
let wordlist = undefined;

//Runtime
let running = false;

function load() {
    try {
        if (window.self === window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener(function (request, sender, sendResponse) {
        if (request.storage !== undefined) {
            sendResponse({storage_enabled: enableStorage, answer_time: answerTime});
            return true;
        }

        if (request.set_answer_time !== undefined) {
            saveCookie("Answer_Delay", request.set_answer_time, 90);
            answerTime = request.set_answer_time;
        }

        if (request.set_storage_enabled !== undefined) {
            saveCookie("Storage_Enabled", request.set_storage_enabled, 90);
            enableStorage = request.set_storage_enabled;
        }

        if (request.progress !== undefined) {
            if(running)
                sendResponse({progress: getPercentage()});
            else
                sendResponse({progress: -1});

            return true;
        }

        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON") {
                running = true;
                main();
            } else if (request.run_mode === "OFF") {
                running = false;
            }
        }

        sendResponse();
        return true;
    });

    setTimeout(function () {
        enableStorage = getCookie("Storage_Enabled", "true") === "true";
        answerTime = parseInt(getCookie("Answer_Delay", "500"));
        wordlist = getWordlist();
    }, 10);
}

function main() {
    if (!running || getPercentage() === 100) {
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

    if (column !== undefined)
        question = column.innerText + "\\" + question;

    if (wordlist[question] !== undefined) {
        pressSampleKey(inputField);
        inputField.value = wordlist[question];

        setTimeout(function () {
            submitButton.click();

            setTimeout(function () {
                let correctAnswer = document.getElementsByClassName("drl-term drl-term--open-ended")[0];

                if ((wordlist[question] === undefined) || (correctAnswer !== undefined)) {
                    wordlist[question] = correctAnswer.innerText;

                    if (enableStorage)
                        saveWordlist();

                    document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
                    setTimeout(main, answerTime);
                } else {
                    main();
                }
            }, answerTime);
        }, answerTime);
    } else {
        retrieveAnswer();
    }
}

function retrieveAnswer() {
    let idkButton = document.getElementsByClassName("question-component__button question-component__button--dontknow")[0];
    let column = document.getElementsByClassName("question-component__tell__name")[0];
    let columnName = column === undefined ? "" : column.innerText + "\\";

    idkButton.click();

    setTimeout(function () {
        let question = document.getElementsByClassName("dwc-markup-text")[0];
        let answer = document.getElementsByClassName("dwc-markup-text")[1];

        wordlist[columnName + question.innerText] = answer.innerText;
        document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();

        if (enableStorage)
            saveWordlist();

        setTimeout(main, answerTime);
    }, answerTime);
}

//Storage
function saveCookie(name, text, days) {
    let date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    document.cookie = "DrillsterBot_" + name + "=" + text + ";expires=" + date.toUTCString() + ";path=/"
}

function getCookie(name, defaultValue) {
    let cookieName = "DrillsterBot_" + name + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    let result = "";

    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];

        while (c.charAt(0) === ' ') {
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

function getWordlist() {
    const wordlist = {};
    const result = getCookie("Wordlist", "");
    let wordsSplit = result.split('|');

    if (result.length === 0)
        return wordlist;

    for (let i = 0; i < wordsSplit.length; i++) {
        let dictionaryValue = wordsSplit[i];
        let question = dictionaryValue.split(':')[0];
        wordlist[question] = dictionaryValue.split(':')[1];
    }

    return wordlist;
}

function saveWordlist() {
    let text = "";

    for (let [question, answer] of Object.entries(wordlist)) {
        if (!(text === "")) {
            text = text + "|";
        }

        text = text + question + ":" + answer;
    }

    saveCookie("Wordlist", text, 90);
}

//Util
function pressSampleKey(inputField) {
    inputField.dispatchEvent(new KeyboardEvent('keydown', {'key': 'a'}));
    inputField.dispatchEvent(new KeyboardEvent('keyup', {'key': 'a'}));
}

function getPercentage() {
    let percentage = document.getElementsByClassName("proficiency-meter__percentage")[0];
    return percentage === undefined ? 100 : parseInt(percentage.innerText.replaceAll("%", ""));
}

load();