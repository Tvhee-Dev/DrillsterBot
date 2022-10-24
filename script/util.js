function pressSampleKey(inputField) {
    inputField.dispatchEvent(new KeyboardEvent('keydown', {'key': 'a'}));
    inputField.dispatchEvent(new KeyboardEvent('keyup', {'key': 'a'}));
}

function checkPercentage() {
    let percentage = document.getElementsByClassName("proficiency-meter__percentage")[0];
    return percentage === undefined || percentage.innerText === "100%";
}