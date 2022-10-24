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