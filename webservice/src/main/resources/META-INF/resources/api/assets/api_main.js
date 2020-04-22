function switchFrame(url, element) {
    let activeElements = document.getElementsByClassName('is-active');

    for (let item of activeElements) {
      item.classList.remove('is-active');
    }

    document.getElementById('iframe').src = url;
    element.classList.add('is-active');
}

window.onload = function() {
    document.getElementById('json-api_switch').onclick = function() {
        switchFrame('/api/swagger/index.html', this)
    };
    document.getElementById('graphiql_switch').onclick = function() {
      switchFrame('/api/graphiql/index.html', this)
    };
}