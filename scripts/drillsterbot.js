const wordlist = {};
let startTime = undefined;
let running = false;

chrome.runtime.onMessage.addListener(
    function (request, sender, sendResponse) {
        if(document.getElementsByClassName("dwc-text-field__input")[0] === undefined)
            return;

        if (request.run_mode === "ON") {
            running = true;

            if(startTime === undefined) {
                start();
            }
            else {
                main();
            }
        } else if (request.run_mode === "OFF") {
            running = false;
            pause();
        }

        sendResponse({run_accepted: running});
    }
);

function start() {
    alert("Welkom bij DrillsterBot! Het gebruik voor toetsen is NIET toegestaan! Druk CTRL + Z om het programma te pauzeren. DrillsterBot is gemaakt door Tvhee#4828.");

    getWordlist();
    startTime = performance.now();
    main();
}

function main(timeout) {
    if (!running) {
        return;
    }

    setTimeout(function () {
        let percentage = document.getElementsByClassName("proficiency-meter__percentage")[0];

        console.log("Percentage " + percentage);

        if (percentage === undefined || percentage.innerText === "100%") {
            const timeDelta = (performance.now() - startTime) / 1000;
            const minutes = parseInt(timeDelta / 60, 10);
            const seconds = parseInt(timeDelta - (60 * minutes), 10);

            alert("DrillsterBot has finished! Took " + minutes + " minutes and " + seconds + " seconds!");
            running = false;
            return;
        }

        let flipText = document.getElementsByClassName("drl-introduction__helper-text")[0];

        if (flipText !== undefined) {
            setTimeout(function () {
                let flipQuestion = document.getElementsByClassName("dwc-markup-text")[0];

                if (flipQuestion !== undefined) {
                    let flipButton = document.getElementsByClassName("dwc-button__shortcut")[0];
                    flipButton.click();

                    setTimeout(function () {
                        let answer = document.getElementsByClassName("dwc-markup-text")[1];

                        wordlist[flipQuestion.innerText] = answer.innerText;
                        document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();

                        main(true);
                    }, 500);
                }
            }, 500);
        } else {
            const question = document.getElementsByClassName("question-component__ask__term")[0].innerText;
            let inputfield = document.getElementsByClassName("dwc-text-field__input")[0];
            let submitbutton = document.getElementsByClassName("drl-enlarged-button")[0];

            inputfield.dispatchEvent(new KeyboardEvent('keydown', {'key': 'a'}));
            inputfield.dispatchEvent(new KeyboardEvent('keyup', {'key': 'a'}));

            if (wordlist[question] !== undefined) {
                inputfield.value = wordlist[question];

                setTimeout(function () {
                    submitbutton.click();

                    setTimeout(function () {
                        let correctAnswer = document.getElementsByClassName("drl-term drl-term--open-ended")[0];

                        if ((wordlist[question] === undefined) || (correctAnswer !== undefined)) {

                            setTimeout(function () {
                                wordlist[question] = correctAnswer.innerText;
                                saveWordlist();

                                document.getElementsByClassName("dwc-button dwc-button--contained")[0].click();
                                main(true);
                            }, 500);
                        } else {
                            main();
                        }
                    }, 500);
                }, 500);
            } else {
                let idkButton = document.getElementsByClassName("question-component__button question-component__button--dontknow")[0];

                idkButton.click();
                main(true);
            }
        }
    }, timeout ? 500 : 0);
}

function pause() {
    setTimeout(function () {
        alert("DrillsterBot is gepauzeerd! Druk weer op CTRL + Z om de bot weer te hervatten!");
    }, 1501)
}

function getWordlist() {
    let name = "DrillsterBot=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    let result = "";

    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];

        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }

        if (c.indexOf(name) === 0) {
            result = c.substring(name.length, c.length);
        }
    }

    if (result === "") {
        return;
    }

    let wordsSplit = result.split('|');

    for (let i = 0; i < wordsSplit.length; i++) {
        let dictionaryValue = wordsSplit[i];
        let question = dictionaryValue.split(':')[0];
        wordlist[question] = dictionaryValue.split(':')[1];
    }
}

function saveWordlist() {
    let result = "";
    let date = new Date();
    date.setTime(date.getTime() + (90 * 24 * 60 * 60 * 1000)); //90 days
    let expires = "expires=" + date.toUTCString();

    for (let [question, answer] of Object.entries(wordlist)) {
        if (!(result === "")) {
            result = result + "|";
        }

        result = result + question + ":" + answer;
    }

    document.cookie = "DrillsterBot=" + result + ";" + expires + ";path=/";
}