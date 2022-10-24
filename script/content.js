let enableStorage = undefined;
let answerTime = undefined;
let wordlist = undefined;
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

        if(request.check_running !== undefined) {
            sendResponse({is_running: running});
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
        answerTime = parseInt(getCookie("Answer_Delay", "300"));
        wordlist = getWordlist();
    }, 10);
}

function main() {
    if (!running) {
        return;
    } else if (checkPercentage()) {
        alert("DrillsterBot is klaar met deze Drill!");
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

    if(column !== undefined)
        question = column.innerText + "/" + question;

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
    let columnName = column === undefined ? "" : column.innerText + "/";

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

load();